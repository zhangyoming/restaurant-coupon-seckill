package com.zym.restaurant.coupon.controller.admin;

import com.zym.restaurant.coupon.common.CommonResult;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.store.vo.StorePageReqVO;
import com.zym.restaurant.coupon.controller.admin.store.vo.StoreRespVO;
import com.zym.restaurant.coupon.controller.admin.store.vo.StoreSaveReqVO;
import com.zym.restaurant.coupon.service.store.RestaurantStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端 - 餐饮门店")
@RestController
@RequestMapping("/admin/store")
@RequiredArgsConstructor
@Validated
public class StoreAdminController {

    private final RestaurantStoreService storeService;

    @PostMapping("/create")
    @Operation(summary = "创建门店")
    public CommonResult<Long> createStore(@Valid @RequestBody StoreSaveReqVO reqVO) {
        return CommonResult.success(storeService.createStore(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改门店")
    public CommonResult<Boolean> updateStore(@Valid @RequestBody StoreSaveReqVO reqVO) {
        storeService.updateStore(reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店")
    public CommonResult<Boolean> deleteStore(@RequestParam("id") @NotNull Long id) {
        storeService.deleteStore(id);
        return CommonResult.success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店")
    public CommonResult<StoreRespVO> getStore(@RequestParam("id") @NotNull Long id) {
        return CommonResult.success(storeService.getStore(id));
    }

    @GetMapping("/page")
    @Operation(summary = "门店分页")
    public CommonResult<PageResult<StoreRespVO>> getStorePage(@Valid StorePageReqVO reqVO) {
        return CommonResult.success(storeService.getStorePage(reqVO));
    }
}
