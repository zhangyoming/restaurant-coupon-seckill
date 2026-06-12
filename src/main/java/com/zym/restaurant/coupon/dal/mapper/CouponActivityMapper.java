package com.zym.restaurant.coupon.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.restaurant.coupon.dal.dataobject.CouponActivityDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponActivityMapper extends BaseMapper<CouponActivityDO> {

    /**
     * 数据库兜底扣库存：available_stock > 0 才扣减，避免数据库层超卖。
     * 第 4 阶段 MQ 异步发券消费者使用，作为数据库层防超卖兜底。
     */
    @Update("""
            UPDATE coupon_activity
            SET available_stock = available_stock - 1,
                version = version + 1,
                update_time = NOW()
            WHERE id = #{activityId}
              AND available_stock > 0
              AND deleted = 0
            """)
    int deductStockIfAvailable(@Param("activityId") Long activityId);

    /**
     * MQ 异步发券数据库落库异常时，回补数据库库存。
     * 注意：只有数据库库存已经扣减成功、但后续发券落库失败时才允许调用。
     */
    @Update("""
            UPDATE coupon_activity
            SET available_stock = available_stock + 1,
                version = version + 1,
                update_time = NOW()
            WHERE id = #{activityId}
              AND deleted = 0
            """)
    int restoreStock(@Param("activityId") Long activityId);
}

