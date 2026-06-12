package com.zym.restaurant.coupon.service.record;

import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordPageReqVO;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordRespVO;

public interface CouponRecordService {

    PageResult<CouponRecordRespVO> getRecordPage(CouponRecordPageReqVO reqVO);

    PageResult<CouponRecordRespVO> getMyCouponPage(Long userId, CouponRecordPageReqVO reqVO);
}
