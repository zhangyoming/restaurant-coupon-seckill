package com.zym.restaurant.coupon.service.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zym.restaurant.coupon.common.exception.BusinessException;
import com.zym.restaurant.coupon.common.exception.ErrorCodeConstants;
import com.zym.restaurant.coupon.dal.dataobject.CouponActivityDO;
import com.zym.restaurant.coupon.dal.mapper.CouponActivityMapper;
import com.zym.restaurant.coupon.dal.mapper.CouponRecordMapper;
import com.zym.restaurant.coupon.enums.CouponActivityStatusEnum;
import com.zym.restaurant.coupon.enums.CouponSeckillLuaResultEnum;
import com.zym.restaurant.coupon.enums.SeckillResultEnum;
import com.zym.restaurant.coupon.redis.key.CouponRedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CouponActivityCacheServiceImpl implements CouponActivityCacheService {

    /**
     * Lua 原子抢券脚本。
     * 返回值：0 成功；1 库存不足；2 重复领取；3 活动未预热。
     */
    private static final DefaultRedisScript<Long> COUPON_SECKILL_SCRIPT = new DefaultRedisScript<>(
            """
                    local stockKey = KEYS[1]
                    local userSetKey = KEYS[2]
                    local soldOutKey = KEYS[3]
                    local userId = ARGV[1]

                    local function setWithKeepTtl(key, value)
                        local ttl = redis.call('PTTL', key)
                        if ttl ~= nil and ttl > 0 then
                            redis.call('PSETEX', key, ttl, value)
                        else
                            redis.call('SET', key, value)
                        end
                    end

                    local stockValue = redis.call('GET', stockKey)
                    if stockValue == false or stockValue == nil then
                        return 3
                    end

                    local soldOut = redis.call('GET', soldOutKey)
                    if soldOut == 'true' then
                        return 1
                    end

                    local exists = redis.call('SISMEMBER', userSetKey, userId)
                    if exists == 1 then
                        return 2
                    end

                    local stock = tonumber(stockValue)
                    if stock == nil or stock <= 0 then
                        setWithKeepTtl(soldOutKey, 'true')
                        return 1
                    end

                    local newStock = redis.call('DECR', stockKey)
                    redis.call('SADD', userSetKey, userId)

                    if newStock <= 0 then
                        setWithKeepTtl(soldOutKey, 'true')
                    end

                    return 0
                    """,
            Long.class);

    /** 活动结束后仍保留 1 天缓存，方便短时间内查询状态和防重复 */
    private static final Duration CACHE_KEEP_AFTER_END = Duration.ofDays(1);

    /** 已过期或没有结束时间时的兜底 TTL */
    private static final Duration DEFAULT_TTL = Duration.ofHours(2);

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponActivityMapper activityMapper;
    private final CouponRecordMapper recordMapper;

    @Override
    public void preheatActivity(Long activityId) {
        CouponActivityDO activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_EXISTS, "优惠券活动不存在");
        }
        if (!CouponActivityStatusEnum.PUBLISHED.getStatus().equals(activity.getStatus())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_PUBLISHED, "只有已发布活动才能预热到 Redis");
        }

        Duration ttl = calculateTtl(activity.getEndTime());
        Integer availableStock = Math.max(Objects.requireNonNullElse(activity.getAvailableStock(), 0), 0);

        String infoKey = CouponRedisKeyConstants.activityInfoKey(activityId);
        String stockKey = CouponRedisKeyConstants.activityStockKey(activityId);
        String userSetKey = CouponRedisKeyConstants.activityUserSetKey(activityId);
        String soldOutKey = CouponRedisKeyConstants.activitySoldOutKey(activityId);

        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("id", String.valueOf(activity.getId()));
        infoMap.put("storeId", String.valueOf(activity.getStoreId()));
        infoMap.put("title", activity.getTitle());
        infoMap.put("couponName", activity.getCouponName());
        infoMap.put("totalStock", String.valueOf(activity.getTotalStock()));
        infoMap.put("availableStock", String.valueOf(availableStock));
        infoMap.put("startTime", String.valueOf(activity.getStartTime()));
        infoMap.put("endTime", String.valueOf(activity.getEndTime()));
        infoMap.put("status", String.valueOf(activity.getStatus()));

        stringRedisTemplate.delete(infoKey);
        stringRedisTemplate.opsForHash().putAll(infoKey, infoMap);
        stringRedisTemplate.expire(infoKey, ttl);

        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(availableStock), ttl);
        stringRedisTemplate.opsForValue().set(soldOutKey, String.valueOf(availableStock <= 0), ttl);

        // 重新预热时先清理旧用户集合，再从数据库加载已领取用户，避免缓存脏数据。
        stringRedisTemplate.delete(userSetKey);
        List<Long> receivedUserIds = recordMapper.selectReceivedUserIdsByActivityId(activityId);
        if (!CollectionUtils.isEmpty(receivedUserIds)) {
            String[] userIdArray = receivedUserIds.stream().map(String::valueOf).toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(userSetKey, userIdArray);
        }
        stringRedisTemplate.expire(userSetKey, ttl);
    }

    @Override
    public int preheatPublishedActivities() {
        List<CouponActivityDO> activities = activityMapper.selectList(new LambdaQueryWrapper<CouponActivityDO>()
                .eq(CouponActivityDO::getStatus, CouponActivityStatusEnum.PUBLISHED.getStatus())
                .ge(CouponActivityDO::getEndTime, LocalDateTime.now())
                .orderByAsc(CouponActivityDO::getStartTime));
        for (CouponActivityDO activity : activities) {
            preheatActivity(activity.getId());
        }
        return activities.size();
    }

    @Override
    public void evictActivity(Long activityId) {
        stringRedisTemplate.delete(List.of(
                CouponRedisKeyConstants.activityInfoKey(activityId),
                CouponRedisKeyConstants.activityStockKey(activityId),
                CouponRedisKeyConstants.activityUserSetKey(activityId),
                CouponRedisKeyConstants.activitySoldOutKey(activityId)
        ));
    }

    @Override
    public boolean isActivityCached(Long activityId) {
        Boolean hasStockKey = stringRedisTemplate.hasKey(CouponRedisKeyConstants.activityStockKey(activityId));
        return Boolean.TRUE.equals(hasStockKey);
    }

    @Override
    public Integer getStock(Long activityId) {
        String value = stringRedisTemplate.opsForValue().get(CouponRedisKeyConstants.activityStockKey(activityId));
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public boolean isSoldOut(Long activityId) {
        String value = stringRedisTemplate.opsForValue().get(CouponRedisKeyConstants.activitySoldOutKey(activityId));
        return Boolean.parseBoolean(value);
    }

    @Override
    public boolean hasUserReceived(Long activityId, Long userId) {
        Boolean member = stringRedisTemplate.opsForSet()
                .isMember(CouponRedisKeyConstants.activityUserSetKey(activityId), String.valueOf(userId));
        return Boolean.TRUE.equals(member);
    }


    @Override
    public CouponSeckillLuaResultEnum trySeckillByLua(Long activityId, Long userId) {
        Long result = stringRedisTemplate.execute(
                COUPON_SECKILL_SCRIPT,
                List.of(
                        CouponRedisKeyConstants.activityStockKey(activityId),
                        CouponRedisKeyConstants.activityUserSetKey(activityId),
                        CouponRedisKeyConstants.activitySoldOutKey(activityId)
                ),
                String.valueOf(userId)
        );
        return CouponSeckillLuaResultEnum.of(result);
    }

    @Override
    public void compensateSeckillReservation(Long activityId, Long userId) {
        if (!isActivityCached(activityId)) {
            return;
        }
        String stockKey = CouponRedisKeyConstants.activityStockKey(activityId);
        String userSetKey = CouponRedisKeyConstants.activityUserSetKey(activityId);
        String soldOutKey = CouponRedisKeyConstants.activitySoldOutKey(activityId);

        Long removed = stringRedisTemplate.opsForSet().remove(userSetKey, String.valueOf(userId));
        if (removed == null || removed <= 0) {
            return;
        }
        Long stock = stringRedisTemplate.opsForValue().increment(stockKey);
        if (stock != null && stock > 0) {
            setKeepingExpire(soldOutKey, "false");
        }
    }

    @Override
    public void markSeckillSuccess(Long activityId, Long userId) {
        if (!isActivityCached(activityId)) {
            return;
        }
        String stockKey = CouponRedisKeyConstants.activityStockKey(activityId);
        String userSetKey = CouponRedisKeyConstants.activityUserSetKey(activityId);
        String soldOutKey = CouponRedisKeyConstants.activitySoldOutKey(activityId);

        Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
        stringRedisTemplate.opsForSet().add(userSetKey, String.valueOf(userId));
        if (stock != null && stock <= 0) {
            setKeepingExpire(soldOutKey, "true");
        }
    }

    @Override
    public void markSoldOut(Long activityId) {
        if (!isActivityCached(activityId)) {
            return;
        }
        setKeepingExpire(CouponRedisKeyConstants.activityStockKey(activityId), "0");
        setKeepingExpire(CouponRedisKeyConstants.activitySoldOutKey(activityId), "true");
    }

    @Override
    public void setSeckillResult(Long activityId, Long userId, SeckillResultEnum resultEnum) {
        String key = CouponRedisKeyConstants.seckillResultKey(activityId, userId);
        stringRedisTemplate.opsForValue().set(key, resultEnum.getCode(), Duration.ofDays(2));
    }

    @Override
    public SeckillResultEnum getSeckillResult(Long activityId, Long userId) {
        String value = stringRedisTemplate.opsForValue().get(CouponRedisKeyConstants.seckillResultKey(activityId, userId));
        if (value == null) {
            return null;
        }
        for (SeckillResultEnum resultEnum : SeckillResultEnum.values()) {
            if (resultEnum.getCode().equals(value)) {
                return resultEnum;
            }
        }
        return null;
    }

    private void setKeepingExpire(String key, String value) {
        Long expireSeconds = stringRedisTemplate.getExpire(key);
        if (expireSeconds != null && expireSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
            return;
        }
        stringRedisTemplate.opsForValue().set(key, value);
    }

    private Duration calculateTtl(LocalDateTime endTime) {
        if (endTime == null) {
            return DEFAULT_TTL;
        }
        long expireAtMillis = endTime.plus(CACHE_KEEP_AFTER_END)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        long ttlMillis = expireAtMillis - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return DEFAULT_TTL;
        }
        return Duration.ofMillis(ttlMillis);
    }
}
