package com.zym.restaurant.coupon.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zym.restaurant.coupon.dal.dataobject.RestaurantStoreDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RestaurantStoreMapper extends BaseMapper<RestaurantStoreDO> {
}
