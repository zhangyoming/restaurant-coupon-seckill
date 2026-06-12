package com.zym.restaurant.coupon.controller.admin;

import com.zym.restaurant.coupon.common.CommonResult;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityPageReqVO;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityRespVO;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivitySaveReqVO;
import com.zym.restaurant.coupon.service.activity.CouponActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端 - 优惠券活动")
@RestController
@RequestMapping("/admin/coupon/activity")
@RequiredArgsConstructor
@Validated
public class CouponActivityAdminController {

    private final CouponActivityService activityService;

    @PostMapping("/create")
    @Operation(summary = "创建优惠券活动")
    public CommonResult<Long> createActivity(@Valid @RequestBody CouponActivitySaveReqVO reqVO) {
        return CommonResult.success(activityService.createActivity(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改优惠券活动")
    public CommonResult<Boolean> updateActivity(@Valid @RequestBody CouponActivitySaveReqVO reqVO) {
        activityService.updateActivity(reqVO);
        return CommonResult.success(true);
    }

    @PutMapping("/publish")
    @Operation(summary = "发布优惠券活动，并自动预热 Redis 库存")
    public CommonResult<Boolean> publishActivity(@RequestParam("id") @NotNull Long id) {
        activityService.publishActivity(id);
        return CommonResult.success(true);
    }

    @PutMapping("/offline")
    @Operation(summary = "下架优惠券活动，并清理 Redis 缓存")
    public CommonResult<Boolean> offlineActivity(@RequestParam("id") @NotNull Long id) {
        activityService.offlineActivity(id);
        return CommonResult.success(true);
    }

    @PostMapping("/preheat")
    @Operation(summary = "手动预热单个活动 Redis 库存")
    public CommonResult<Boolean> preheatActivity(@RequestParam("id") @NotNull Long id) {
        activityService.preheatActivity(id);
        return CommonResult.success(true);
    }

    @PostMapping("/preheat-published")
    @Operation(summary = "批量预热所有已发布且未结束活动 Redis 库存")
    public CommonResult<Integer> preheatPublishedActivities() {
        return CommonResult.success(activityService.preheatPublishedActivities());
    }

    @GetMapping("/redis-stock")
    @Operation(summary = "查询 Redis 中的活动库存")
    public CommonResult<Integer> getRedisStock(@RequestParam("id") @NotNull Long id) {
        return CommonResult.success(activityService.getRedisStock(id));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除优惠券活动")
    public CommonResult<Boolean> deleteActivity(@RequestParam("id") @NotNull Long id) {
        activityService.deleteActivity(id);
        return CommonResult.success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得优惠券活动")
    public CommonResult<CouponActivityRespVO> getActivity(@RequestParam("id") @NotNull Long id) {
        return CommonResult.success(activityService.getActivity(id));
    }

    @GetMapping("/page")
    @Operation(summary = "优惠券活动分页")
    public CommonResult<PageResult<CouponActivityRespVO>> getActivityPage(@Valid CouponActivityPageReqVO reqVO) {
        return CommonResult.success(activityService.getActivityPage(reqVO));
    }
}
