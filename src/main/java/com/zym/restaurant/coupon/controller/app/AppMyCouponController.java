package com.zym.restaurant.coupon.controller.app;

import com.zym.restaurant.coupon.common.CommonResult;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordPageReqVO;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordRespVO;
import com.zym.restaurant.coupon.service.record.CouponRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端 - 我的优惠券")
@RestController
@RequestMapping("/app/coupon/my")
@RequiredArgsConstructor
@Validated
public class AppMyCouponController {

    private final CouponRecordService recordService;

    @GetMapping("/page")
    @Operation(summary = "我的优惠券分页")
    public CommonResult<PageResult<CouponRecordRespVO>> getMyCouponPage(@RequestParam("userId") @NotNull Long userId,
                                                                        @Valid CouponRecordPageReqVO reqVO) {
        return CommonResult.success(recordService.getMyCouponPage(userId, reqVO));
    }
}
