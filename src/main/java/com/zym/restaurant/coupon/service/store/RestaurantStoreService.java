package com.zym.restaurant.coupon.service.store;

import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.store.vo.StorePageReqVO;
import com.zym.restaurant.coupon.controller.admin.store.vo.StoreRespVO;
import com.zym.restaurant.coupon.controller.admin.store.vo.StoreSaveReqVO;

public interface RestaurantStoreService {

    Long createStore(StoreSaveReqVO reqVO);

    void updateStore(StoreSaveReqVO reqVO);

    void deleteStore(Long id);

    StoreRespVO getStore(Long id);

    PageResult<StoreRespVO> getStorePage(StorePageReqVO reqVO);

    void validateEnabledStore(Long id);
}
