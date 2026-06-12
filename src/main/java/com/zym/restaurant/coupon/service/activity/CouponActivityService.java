package com.zym.restaurant.coupon.service.activity;

import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityPageReqVO;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityRespVO;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivitySaveReqVO;
import com.zym.restaurant.coupon.controller.app.activity.vo.AppCouponActivityRespVO;

public interface CouponActivityService {

    Long createActivity(CouponActivitySaveReqVO reqVO);

    void updateActivity(CouponActivitySaveReqVO reqVO);

    void publishActivity(Long id);

    void offlineActivity(Long id);

    void deleteActivity(Long id);

    void preheatActivity(Long id);

    int preheatPublishedActivities();

    Integer getRedisStock(Long id);

    CouponActivityRespVO getActivity(Long id);

    PageResult<CouponActivityRespVO> getActivityPage(CouponActivityPageReqVO reqVO);

    PageResult<AppCouponActivityRespVO> getAppActivityPage(CouponActivityPageReqVO reqVO);
}
