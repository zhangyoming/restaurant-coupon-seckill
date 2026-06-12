package com.zym.restaurant.coupon.service.cache;

import com.zym.restaurant.coupon.enums.CouponSeckillLuaResultEnum;
import com.zym.restaurant.coupon.enums.SeckillResultEnum;

/**
 * 优惠券活动 Redis 缓存 Service。
 *
 * 第 4 阶段职责：
 * 1. 将已发布活动库存、售罄标记、已领取用户集合预热到 Redis；
 * 2. 通过 Redis Lua 原子完成库存判断、重复领取判断、扣减库存、记录用户；
 * 3. 当数据库落库失败时，对 Redis 预扣库存做补偿；
 * 4. MQ 异步发券落库失败时，对 Redis 预扣库存做补偿。
 */
public interface CouponActivityCacheService {

    /** 预热单个活动库存和已领取用户集合 */
    void preheatActivity(Long activityId);

    /** 预热所有已发布活动，返回预热数量 */
    int preheatPublishedActivities();

    /** 清除单个活动缓存 */
    void evictActivity(Long activityId);

    /** 活动是否已经预热 */
    boolean isActivityCached(Long activityId);

    /** 获得 Redis 中的活动剩余库存；未预热返回 null */
    Integer getStock(Long activityId);

    /** Redis 中是否标记售罄 */
    boolean isSoldOut(Long activityId);

    /** Redis 中是否已经记录该用户领取过 */
    boolean hasUserReceived(Long activityId, Long userId);

    /**
     * 使用 Lua 原子抢券：库存判断 + 重复领取判断 + 扣减库存 + 记录用户。
     */
    CouponSeckillLuaResultEnum trySeckillByLua(Long activityId, Long userId);

    /**
     * 数据库落库失败时补偿 Redis 预扣库存。
     * 只有成功移除用户领取标记时才回补库存，避免重复补偿。
     */
    void compensateSeckillReservation(Long activityId, Long userId);

    /** 数据库抢券成功后刷新 Redis 库存、已领取用户集合和售罄标记；兼容旧同步流程使用 */
    void markSeckillSuccess(Long activityId, Long userId);

    /** 标记活动售罄 */
    void markSoldOut(Long activityId);

    /** 写入用户抢券结果缓存，用于 MQ 异步发券后快速查询 */
    void setSeckillResult(Long activityId, Long userId, SeckillResultEnum resultEnum);

    /** 查询用户抢券结果缓存；没有结果返回 null */
    SeckillResultEnum getSeckillResult(Long activityId, Long userId);
}
