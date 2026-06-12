package com.zym.restaurant.coupon.convert;

import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityRespVO;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivitySaveReqVO;
import com.zym.restaurant.coupon.controller.app.activity.vo.AppCouponActivityRespVO;
import com.zym.restaurant.coupon.dal.dataobject.CouponActivityDO;
import com.zym.restaurant.coupon.enums.CouponActivityStatusEnum;

public class CouponActivityConvert {

    public static CouponActivityDO toDO(CouponActivitySaveReqVO vo) {
        CouponActivityDO activity = new CouponActivityDO();
        activity.setId(vo.getId());
        activity.setStoreId(vo.getStoreId());
        activity.setTitle(vo.getTitle());
        activity.setCouponName(vo.getCouponName());
        activity.setCouponAmount(vo.getCouponAmount());
        activity.setThresholdAmount(vo.getThresholdAmount());
        activity.setTotalStock(vo.getTotalStock());
        activity.setAvailableStock(vo.getTotalStock());
        activity.setPerUserLimit(vo.getPerUserLimit());
        activity.setStartTime(vo.getStartTime());
        activity.setEndTime(vo.getEndTime());
        activity.setStatus(CouponActivityStatusEnum.DRAFT.getStatus());
        return activity;
    }

    public static CouponActivityRespVO toRespVO(CouponActivityDO activity) {
        if (activity == null) {
            return null;
        }
        CouponActivityRespVO vo = new CouponActivityRespVO();
        vo.setId(activity.getId());
        vo.setStoreId(activity.getStoreId());
        vo.setTitle(activity.getTitle());
        vo.setCouponName(activity.getCouponName());
        vo.setCouponAmount(activity.getCouponAmount());
        vo.setThresholdAmount(activity.getThresholdAmount());
        vo.setTotalStock(activity.getTotalStock());
        vo.setAvailableStock(activity.getAvailableStock());
        vo.setPerUserLimit(activity.getPerUserLimit());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setCreateTime(activity.getCreateTime());
        return vo;
    }

    public static AppCouponActivityRespVO toAppRespVO(CouponActivityDO activity) {
        if (activity == null) {
            return null;
        }
        AppCouponActivityRespVO vo = new AppCouponActivityRespVO();
        vo.setId(activity.getId());
        vo.setStoreId(activity.getStoreId());
        vo.setTitle(activity.getTitle());
        vo.setCouponName(activity.getCouponName());
        vo.setCouponAmount(activity.getCouponAmount());
        vo.setThresholdAmount(activity.getThresholdAmount());
        vo.setAvailableStock(activity.getAvailableStock());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        return vo;
    }
}
