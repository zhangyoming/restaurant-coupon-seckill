package com.zym.restaurant.coupon.controller.admin;

import com.zym.restaurant.coupon.common.CommonResult;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordPageReqVO;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordRespVO;
import com.zym.restaurant.coupon.service.record.CouponRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端 - 优惠券领取记录")
@RestController
@RequestMapping("/admin/coupon/record")
@RequiredArgsConstructor
public class CouponRecordAdminController {

    private final CouponRecordService recordService;

    @GetMapping("/page")
    @Operation(summary = "优惠券领取记录分页")
    public CommonResult<PageResult<CouponRecordRespVO>> getRecordPage(@Valid CouponRecordPageReqVO reqVO) {
        return CommonResult.success(recordService.getRecordPage(reqVO));
    }
}
