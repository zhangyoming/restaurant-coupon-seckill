package com.zym.restaurant.coupon.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 秒杀访问控制配置。
 *
 * 这些配置用于第 5 阶段的接口限流、防刷、验证码和秒杀令牌。
 */
@Data
@Component
@ConfigurationProperties(prefix = "restaurant.coupon.seckill")
public class SeckillAccessProperties {

    /** 验证码有效期，单位：秒 */
    private long captchaExpireSeconds = 120;

    /** 秒杀令牌有效期，单位：秒 */
    private long tokenExpireSeconds = 60;

    /** 用户抢券接口限流次数 */
    private int userLimitTimes = 5;

    /** 用户抢券接口限流窗口，单位：秒 */
    private int userLimitWindowSeconds = 10;

    /** IP 抢券接口限流次数 */
    private int ipLimitTimes = 60;

    /** IP 抢券接口限流窗口，单位：秒 */
    private int ipLimitWindowSeconds = 10;

    /** 用户创建秒杀令牌限流次数 */
    private int tokenCreateLimitTimes = 3;

    /** 用户创建秒杀令牌限流窗口，单位：秒 */
    private int tokenCreateLimitWindowSeconds = 60;

    /** 用户获取验证码限流次数 */
    private int captchaLimitTimes = 10;

    /** 用户获取验证码限流窗口，单位：秒 */
    private int captchaLimitWindowSeconds = 60;

    /** 是否启用秒杀令牌校验；本地联调可临时关闭，压测和演示建议开启 */
    private boolean tokenRequired = true;
}
