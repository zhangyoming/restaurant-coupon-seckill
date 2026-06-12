package com.zym.restaurant.coupon.convert;

import com.zym.restaurant.coupon.controller.admin.store.vo.StoreRespVO;
import com.zym.restaurant.coupon.controller.admin.store.vo.StoreSaveReqVO;
import com.zym.restaurant.coupon.dal.dataobject.RestaurantStoreDO;

public class StoreConvert {

    public static RestaurantStoreDO toDO(StoreSaveReqVO vo) {
        RestaurantStoreDO store = new RestaurantStoreDO();
        store.setId(vo.getId());
        store.setName(vo.getName());
        store.setAddress(vo.getAddress());
        store.setPhone(vo.getPhone());
        store.setStatus(vo.getStatus());
        return store;
    }

    public static StoreRespVO toRespVO(RestaurantStoreDO store) {
        if (store == null) {
            return null;
        }
        StoreRespVO vo = new StoreRespVO();
        vo.setId(store.getId());
        vo.setName(store.getName());
        vo.setAddress(store.getAddress());
        vo.setPhone(store.getPhone());
        vo.setStatus(store.getStatus());
        vo.setCreateTime(store.getCreateTime());
        return vo;
    }
}
