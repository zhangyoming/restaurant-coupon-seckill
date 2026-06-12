package com.zym.restaurant.coupon.convert;

import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordRespVO;
import com.zym.restaurant.coupon.dal.dataobject.CouponRecordDO;

public class CouponRecordConvert {

    public static CouponRecordRespVO toRespVO(CouponRecordDO record) {
        if (record == null) {
            return null;
        }
        CouponRecordRespVO vo = new CouponRecordRespVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setActivityId(record.getActivityId());
        vo.setStoreId(record.getStoreId());
        vo.setCouponName(record.getCouponName());
        vo.setCouponAmount(record.getCouponAmount());
        vo.setThresholdAmount(record.getThresholdAmount());
        vo.setStatus(record.getStatus());
        vo.setReceiveTime(record.getReceiveTime());
        return vo;
    }
}
