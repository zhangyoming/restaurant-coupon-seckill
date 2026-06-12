package com.zym.restaurant.coupon.redis.key;

/**
 * 优惠券秒杀 Redis Key 规范。
 *
 * 设计原则：
 * 1. 所有 key 都以 coupon: 开头，避免和其他业务冲突；
 * 2. 活动维度的库存、用户集合、售罄标记分开存储，便于 Lua 原子扣减；
 * 3. MQ 异步发券通过 result key 支持用户轮询查询；
 * 4. 第 5 阶段新增 captcha、token、limit key，用于验证码、秒杀令牌和接口限流。
 */
public final class CouponRedisKeyConstants {

    private CouponRedisKeyConstants() {
    }

    /** 活动基本信息 Hash */
    public static String activityInfoKey(Long activityId) {
        return "coupon:activity:" + activityId + ":info";
    }

    /** 活动剩余库存 String */
    public static String activityStockKey(Long activityId) {
        return "coupon:activity:" + activityId + ":stock";
    }

    /** 活动已领取用户 Set */
    public static String activityUserSetKey(Long activityId) {
        return "coupon:activity:" + activityId + ":users";
    }

    /** 活动售罄标记 String，true / false */
    public static String activitySoldOutKey(Long activityId) {
        return "coupon:activity:" + activityId + ":soldout";
    }

    /** 用户抢券结果 String，MQ 异步发券阶段使用 */
    public static String seckillResultKey(Long activityId, Long userId) {
        return "coupon:seckill:result:" + activityId + ":" + userId;
    }

    /** 秒杀验证码 String，value = userId:activityId:answer */
    public static String seckillCaptchaKey(String captchaId) {
        return "coupon:seckill:captcha:" + captchaId;
    }

    /** 秒杀令牌 String，value = userId:activityId */
    public static String seckillTokenKey(String token) {
        return "coupon:seckill:token:" + token;
    }

    /** 用户维度抢券接口限流 */
    public static String seckillUserLimitKey(Long userId) {
        return "coupon:limit:seckill:user:" + userId;
    }

    /** IP 维度抢券接口限流 */
    public static String seckillIpLimitKey(String clientIp) {
        return "coupon:limit:seckill:ip:" + clientIp;
    }

    /** 用户维度创建秒杀令牌限流 */
    public static String tokenCreateUserLimitKey(Long userId) {
        return "coupon:limit:token:user:" + userId;
    }

    /** IP 维度创建秒杀令牌限流 */
    public static String tokenCreateIpLimitKey(String clientIp) {
        return "coupon:limit:token:ip:" + clientIp;
    }

    /** 用户维度获取验证码限流 */
    public static String captchaUserLimitKey(Long userId) {
        return "coupon:limit:captcha:user:" + userId;
    }

    /** IP 维度获取验证码限流 */
    public static String captchaIpLimitKey(String clientIp) {
        return "coupon:limit:captcha:ip:" + clientIp;
    }
}
