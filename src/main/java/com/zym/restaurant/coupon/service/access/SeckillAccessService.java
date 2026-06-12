package com.zym.restaurant.coupon.service.access;

import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillCaptchaRespVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillTokenCreateReqVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillTokenRespVO;

/** 秒杀访问控制：验证码、秒杀令牌、限流、防刷。 */
public interface SeckillAccessService {

    /** 创建算术验证码。 */
    CouponSeckillCaptchaRespVO createCaptcha(Long userId, Long activityId, String clientIp);

    /** 校验验证码并创建秒杀令牌。 */
    CouponSeckillTokenRespVO createToken(CouponSeckillTokenCreateReqVO reqVO, String clientIp);

    /** 校验抢券访问资格：用户限流、IP 限流、秒杀令牌。 */
    void validateSeckillAccess(Long userId, Long activityId, String token, String clientIp);
}
