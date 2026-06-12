package com.zym.restaurant.coupon.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_record")
public class CouponRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    private Long storeId;

    private String couponName;

    private BigDecimal couponAmount;

    private BigDecimal thresholdAmount;

    /** 状态：0未使用 1已使用 2已过期 */
    private Integer status;

    private LocalDateTime receiveTime;

    private LocalDateTime useTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
