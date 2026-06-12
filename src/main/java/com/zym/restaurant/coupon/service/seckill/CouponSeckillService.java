package com.zym.restaurant.coupon.service.seckill;

import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillReqVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillRespVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillResultRespVO;

public interface CouponSeckillService {

    CouponSeckillRespVO seckill(CouponSeckillReqVO reqVO);

    CouponSeckillResultRespVO getResult(Long userId, Long activityId);
}
