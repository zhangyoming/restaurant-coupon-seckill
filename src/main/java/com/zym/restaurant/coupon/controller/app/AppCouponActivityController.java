package com.zym.restaurant.coupon.controller.app;

import com.zym.restaurant.coupon.common.CommonResult;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityPageReqVO;
import com.zym.restaurant.coupon.controller.app.activity.vo.AppCouponActivityRespVO;
import com.zym.restaurant.coupon.service.activity.CouponActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端 - 优惠券活动")
@RestController
@RequestMapping("/app/coupon/activity")
@RequiredArgsConstructor
public class AppCouponActivityController {

    private final CouponActivityService activityService;

    @GetMapping("/page")
    @Operation(summary = "用户端优惠券活动分页")
    public CommonResult<PageResult<AppCouponActivityRespVO>> getAppActivityPage(@Valid CouponActivityPageReqVO reqVO) {
        return CommonResult.success(activityService.getAppActivityPage(reqVO));
    }
}
