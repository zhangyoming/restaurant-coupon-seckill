package com.zym.restaurant.coupon.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_activity")
public class CouponActivityDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;

    private String title;

    private String couponName;

    private BigDecimal couponAmount;

    private BigDecimal thresholdAmount;

    private Integer totalStock;

    private Integer availableStock;

    private Integer perUserLimit;

    /** 状态：0草稿 1已发布 2已结束 3已下架 */
    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
