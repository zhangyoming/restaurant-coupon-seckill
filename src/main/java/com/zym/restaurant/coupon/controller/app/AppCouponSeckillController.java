package com.zym.restaurant.coupon.controller.app;

import com.zym.restaurant.coupon.common.CommonResult;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillCaptchaRespVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillReqVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillRespVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillResultRespVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillTokenCreateReqVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillTokenRespVO;
import com.zym.restaurant.coupon.framework.util.ClientIpUtils;
import com.zym.restaurant.coupon.service.access.SeckillAccessService;
import com.zym.restaurant.coupon.service.seckill.CouponSeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端 - 优惠券秒杀")
@RestController
@RequestMapping("/app/coupon/seckill")
@RequiredArgsConstructor
@Validated
public class AppCouponSeckillController {

    private final CouponSeckillService seckillService;
    private final SeckillAccessService seckillAccessService;

    @GetMapping("/captcha")
    @Operation(summary = "获取秒杀验证码")
    public CommonResult<CouponSeckillCaptchaRespVO> getCaptcha(@RequestParam("userId") @NotNull Long userId,
                                                               @RequestParam("activityId") @NotNull Long activityId,
                                                               HttpServletRequest request) {
        String clientIp = ClientIpUtils.getClientIp(request);
        return CommonResult.success(seckillAccessService.createCaptcha(userId, activityId, clientIp));
    }

    @PostMapping("/token")
    @Operation(summary = "校验验证码并获取秒杀令牌")
    public CommonResult<CouponSeckillTokenRespVO> createToken(@Valid @RequestBody CouponSeckillTokenCreateReqVO reqVO,
                                                              HttpServletRequest request) {
        String clientIp = ClientIpUtils.getClientIp(request);
        return CommonResult.success(seckillAccessService.createToken(reqVO, clientIp));
    }

    @PostMapping
    @Operation(summary = "抢券：Redis Lua 原子扣库存 + RabbitMQ 异步发券 + 令牌校验 + 限流")
    public CommonResult<CouponSeckillRespVO> seckill(@Valid @RequestBody CouponSeckillReqVO reqVO,
                                                     HttpServletRequest request) {
        String clientIp = ClientIpUtils.getClientIp(request);
        seckillAccessService.validateSeckillAccess(reqVO.getUserId(), reqVO.getActivityId(), reqVO.getToken(), clientIp);
        return CommonResult.success(seckillService.seckill(reqVO));
    }

    @GetMapping("/result")
    @Operation(summary = "查询抢券结果")
    public CommonResult<CouponSeckillResultRespVO> getResult(@RequestParam("userId") @NotNull Long userId,
                                                             @RequestParam("activityId") @NotNull Long activityId) {
        return CommonResult.success(seckillService.getResult(userId, activityId));
    }
}
