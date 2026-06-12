package com.zym.restaurant.coupon.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.restaurant.coupon.dal.dataobject.CouponRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponRecordMapper extends BaseMapper<CouponRecordDO> {

    /**
     * 查询活动下已经领取过优惠券的用户编号，用于 Redis 预热已领取用户集合。
     */
    @Select("""
            SELECT user_id
            FROM coupon_record
            WHERE activity_id = #{activityId}
              AND deleted = 0
            """)
    List<Long> selectReceivedUserIdsByActivityId(@Param("activityId") Long activityId);
}
