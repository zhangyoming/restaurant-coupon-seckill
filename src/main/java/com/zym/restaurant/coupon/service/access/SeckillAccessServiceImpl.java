package com.zym.restaurant.coupon.service.access;

import com.zym.restaurant.coupon.common.exception.BusinessException;
import com.zym.restaurant.coupon.common.exception.ErrorCodeConstants;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillCaptchaRespVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillTokenCreateReqVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillTokenRespVO;
import com.zym.restaurant.coupon.dal.dataobject.CouponActivityDO;
import com.zym.restaurant.coupon.dal.mapper.CouponActivityMapper;
import com.zym.restaurant.coupon.enums.CouponActivityStatusEnum;
import com.zym.restaurant.coupon.framework.config.SeckillAccessProperties;
import com.zym.restaurant.coupon.redis.key.CouponRedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 第 5 阶段：接口限流、防刷、秒杀令牌。
 *
 * 设计目标：
 * 1. 抢券接口不允许被用户和 IP 高频刷；
 * 2. 用户必须先通过验证码拿到短期秒杀令牌，再请求真正的抢券接口；
 * 3. 秒杀令牌一次性使用，降低提前构造请求和重复提交的风险；
 * 4. 所有限流计数都在 Redis 里完成，适合多实例部署。
 */
@Service
@RequiredArgsConstructor
public class SeckillAccessServiceImpl implements SeckillAccessService {

    /**
     * Redis 固定窗口限流脚本。
     * 返回值：0 未超限；1 已超限。
     */
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
                    local key = KEYS[1]
                    local limit = tonumber(ARGV[1])
                    local windowSeconds = tonumber(ARGV[2])

                    local current = redis.call('INCR', key)
                    if current == 1 then
                        redis.call('EXPIRE', key, windowSeconds)
                    end

                    if current > limit then
                        return 1
                    end
                    return 0
                    """,
            Long.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponActivityMapper activityMapper;
    private final SeckillAccessProperties accessProperties;

    @Override
    public CouponSeckillCaptchaRespVO createCaptcha(Long userId, Long activityId, String clientIp) {
        validateActivityCanAccess(activityId, false);
        checkCaptchaLimit(userId, clientIp);

        int left = RANDOM.nextInt(9) + 1;
        int right = RANDOM.nextInt(9) + 1;
        boolean plus = RANDOM.nextBoolean();
        int answer = plus ? left + right : Math.max(left, right) - Math.min(left, right);
        String expression = plus ? left + " + " + right + " = ?"
                : Math.max(left, right) + " - " + Math.min(left, right) + " = ?";

        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String value = userId + ":" + activityId + ":" + answer;
        stringRedisTemplate.opsForValue().set(
                CouponRedisKeyConstants.seckillCaptchaKey(captchaId),
                value,
                Duration.ofSeconds(accessProperties.getCaptchaExpireSeconds())
        );

        CouponSeckillCaptchaRespVO respVO = new CouponSeckillCaptchaRespVO();
        respVO.setCaptchaId(captchaId);
        respVO.setExpression(expression);
        respVO.setExpireSeconds(accessProperties.getCaptchaExpireSeconds());
        return respVO;
    }

    @Override
    public CouponSeckillTokenRespVO createToken(CouponSeckillTokenCreateReqVO reqVO, String clientIp) {
        validateActivityCanAccess(reqVO.getActivityId(), true);
        checkTokenCreateLimit(reqVO.getUserId(), clientIp);
        validateCaptcha(reqVO);

        String token = UUID.randomUUID().toString().replace("-", "");
        String value = reqVO.getUserId() + ":" + reqVO.getActivityId();
        stringRedisTemplate.opsForValue().set(
                CouponRedisKeyConstants.seckillTokenKey(token),
                value,
                Duration.ofSeconds(accessProperties.getTokenExpireSeconds())
        );

        CouponSeckillTokenRespVO respVO = new CouponSeckillTokenRespVO();
        respVO.setToken(token);
        respVO.setExpireSeconds(accessProperties.getTokenExpireSeconds());
        return respVO;
    }

    @Override
    public void validateSeckillAccess(Long userId, Long activityId, String token, String clientIp) {
        checkSeckillRateLimit(userId, clientIp);
        if (!accessProperties.isTokenRequired()) {
            return;
        }
        if (!StringUtils.hasText(token)) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_TOKEN_INVALID, "秒杀令牌不能为空");
        }
        String key = CouponRedisKeyConstants.seckillTokenKey(token);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_TOKEN_INVALID, "秒杀令牌不存在或已过期，请重新获取");
        }
        String expected = userId + ":" + activityId;
        if (!expected.equals(value)) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_TOKEN_INVALID, "秒杀令牌与当前用户或活动不匹配");
        }
        // 秒杀令牌一次性使用，防止用户拿同一个 token 重复请求抢券接口。
        stringRedisTemplate.delete(key);
    }

    private void validateCaptcha(CouponSeckillTokenCreateReqVO reqVO) {
        String key = CouponRedisKeyConstants.seckillCaptchaKey(reqVO.getCaptchaId());
        String value = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_CAPTCHA_EXPIRED, "验证码不存在或已过期");
        }
        String[] parts = value.split(":");
        if (parts.length != 3) {
            stringRedisTemplate.delete(key);
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_CAPTCHA_EXPIRED, "验证码状态异常，请重新获取");
        }
        boolean matched = Objects.equals(parts[0], String.valueOf(reqVO.getUserId()))
                && Objects.equals(parts[1], String.valueOf(reqVO.getActivityId()))
                && Objects.equals(parts[2], reqVO.getCaptchaCode().trim());
        stringRedisTemplate.delete(key);
        if (!matched) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_CAPTCHA_INVALID, "验证码错误");
        }
    }

    private void validateActivityCanAccess(Long activityId, boolean requireStarted) {
        CouponActivityDO activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_EXISTS, "优惠券活动不存在");
        }
        if (!CouponActivityStatusEnum.PUBLISHED.getStatus().equals(activity.getStatus())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_PUBLISHED, "优惠券活动未发布");
        }
        LocalDateTime now = LocalDateTime.now();
        if (requireStarted && now.isBefore(activity.getStartTime())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_STARTED, "活动未开始，不能提前获取秒杀令牌");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_ENDED, "活动已结束");
        }
    }

    private void checkCaptchaLimit(Long userId, String clientIp) {
        checkRateLimit(
                CouponRedisKeyConstants.captchaUserLimitKey(userId),
                accessProperties.getCaptchaLimitTimes(),
                accessProperties.getCaptchaLimitWindowSeconds(),
                "验证码获取过于频繁，请稍后再试"
        );
        checkRateLimit(
                CouponRedisKeyConstants.captchaIpLimitKey(clientIp),
                accessProperties.getCaptchaLimitTimes() * 5,
                accessProperties.getCaptchaLimitWindowSeconds(),
                "当前 IP 获取验证码过于频繁，请稍后再试"
        );
    }

    private void checkTokenCreateLimit(Long userId, String clientIp) {
        checkRateLimit(
                CouponRedisKeyConstants.tokenCreateUserLimitKey(userId),
                accessProperties.getTokenCreateLimitTimes(),
                accessProperties.getTokenCreateLimitWindowSeconds(),
                "秒杀令牌获取过于频繁，请稍后再试"
        );
        checkRateLimit(
                CouponRedisKeyConstants.tokenCreateIpLimitKey(clientIp),
                accessProperties.getTokenCreateLimitTimes() * 10,
                accessProperties.getTokenCreateLimitWindowSeconds(),
                "当前 IP 获取秒杀令牌过于频繁，请稍后再试"
        );
    }

    private void checkSeckillRateLimit(Long userId, String clientIp) {
        checkRateLimit(
                CouponRedisKeyConstants.seckillUserLimitKey(userId),
                accessProperties.getUserLimitTimes(),
                accessProperties.getUserLimitWindowSeconds(),
                "抢券请求过于频繁，请稍后再试"
        );
        checkRateLimit(
                CouponRedisKeyConstants.seckillIpLimitKey(clientIp),
                accessProperties.getIpLimitTimes(),
                accessProperties.getIpLimitWindowSeconds(),
                "当前 IP 抢券请求过于频繁，请稍后再试"
        );
    }

    private void checkRateLimit(String key, int limit, int windowSeconds, String message) {
        Long result = stringRedisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(limit),
                String.valueOf(windowSeconds)
        );
        if (result != null && result == 1L) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_RATE_LIMITED, message);
        }
    }
}
