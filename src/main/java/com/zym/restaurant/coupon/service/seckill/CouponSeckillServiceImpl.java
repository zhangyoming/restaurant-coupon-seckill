package com.zym.restaurant.coupon.service.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zym.restaurant.coupon.common.exception.BusinessException;
import com.zym.restaurant.coupon.common.exception.ErrorCodeConstants;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillReqVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillRespVO;
import com.zym.restaurant.coupon.controller.app.seckill.vo.CouponSeckillResultRespVO;
import com.zym.restaurant.coupon.dal.dataobject.CouponActivityDO;
import com.zym.restaurant.coupon.dal.dataobject.CouponSeckillOrderDO;
import com.zym.restaurant.coupon.dal.mapper.CouponActivityMapper;
import com.zym.restaurant.coupon.dal.mapper.CouponSeckillOrderMapper;
import com.zym.restaurant.coupon.enums.CouponActivityStatusEnum;
import com.zym.restaurant.coupon.enums.CouponSeckillLuaResultEnum;
import com.zym.restaurant.coupon.enums.CouponSeckillOrderStatusEnum;
import com.zym.restaurant.coupon.enums.SeckillResultEnum;
import com.zym.restaurant.coupon.framework.transaction.TransactionCallbackUtils;
import com.zym.restaurant.coupon.mq.message.CouponSeckillMessage;
import com.zym.restaurant.coupon.mq.producer.CouponSeckillProducer;
import com.zym.restaurant.coupon.service.cache.CouponActivityCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponSeckillServiceImpl implements CouponSeckillService {

    private final CouponActivityMapper activityMapper;
    private final CouponSeckillOrderMapper orderMapper;
    private final CouponActivityCacheService activityCacheService;
    private final CouponSeckillProducer couponSeckillProducer;

    /**
     * 第 4 阶段：Redis Lua 原子预扣库存 + RabbitMQ 异步发券。
     *
     * 主流程只做高并发入口最必要的事情：
     * 1. 校验活动状态和时间；
     * 2. 通过 Redis Lua 原子完成库存判断、重复领取判断、预扣库存、记录用户；
     * 3. 创建“排队中”的秒杀订单；
     * 4. 事务提交后投递 MQ；
     * 5. 立即返回“排队中”，由消费者异步落库发券。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponSeckillRespVO seckill(CouponSeckillReqVO reqVO) {
        CouponActivityDO activity = validateActivityCanSeckill(reqVO.getActivityId());
        ensureRedisCacheReady(activity.getId());

        CouponSeckillLuaResultEnum luaResult = tryLuaSeckillWithRetry(activity.getId(), reqVO.getUserId());
        if (CouponSeckillLuaResultEnum.SOLD_OUT.equals(luaResult)) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_STOCK_EMPTY, "优惠券已抢完");
        }
        if (CouponSeckillLuaResultEnum.DUPLICATE.equals(luaResult)) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_DUPLICATE, "请勿重复领取该优惠券");
        }
        if (!CouponSeckillLuaResultEnum.SUCCESS.equals(luaResult)) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_CACHE_NOT_READY, "活动库存未预热，请稍后重试");
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        try {
            CouponSeckillOrderDO order = createQueuingOrder(reqVO.getUserId(), activity, requestId);
            activityCacheService.setSeckillResult(activity.getId(), reqVO.getUserId(), SeckillResultEnum.QUEUING);

            CouponSeckillMessage message = buildMessage(reqVO.getUserId(), activity, order, requestId);
            TransactionCallbackUtils.afterCommit(() -> couponSeckillProducer.sendSeckillMessage(message));

            return buildResp(requestId, order.getId(), SeckillResultEnum.QUEUING);
        } catch (DuplicateKeyException ex) {
            // 秒杀订单 user_id + activity_id 唯一索引兜底，避免用户并发重复请求产生多条排队订单。
            activityCacheService.compensateSeckillReservation(activity.getId(), reqVO.getUserId());
            activityCacheService.setSeckillResult(activity.getId(), reqVO.getUserId(), SeckillResultEnum.DUPLICATE);
            throw BusinessException.of(ErrorCodeConstants.COUPON_SECKILL_DUPLICATE, "请勿重复领取该优惠券");
        } catch (RuntimeException ex) {
            activityCacheService.compensateSeckillReservation(activity.getId(), reqVO.getUserId());
            activityCacheService.setSeckillResult(activity.getId(), reqVO.getUserId(), SeckillResultEnum.FAILED);
            throw ex;
        }
    }

    @Override
    public CouponSeckillResultRespVO getResult(Long userId, Long activityId) {
        CouponSeckillOrderDO order = orderMapper.selectOne(new LambdaQueryWrapper<CouponSeckillOrderDO>()
                .eq(CouponSeckillOrderDO::getUserId, userId)
                .eq(CouponSeckillOrderDO::getActivityId, activityId)
                .last("LIMIT 1"));

        CouponSeckillResultRespVO respVO = new CouponSeckillResultRespVO();
        respVO.setUserId(userId);
        respVO.setActivityId(activityId);
        if (order == null) {
            respVO.setResultCode(SeckillResultEnum.NOT_REQUESTED.getCode());
            respVO.setResultMsg(SeckillResultEnum.NOT_REQUESTED.getName());
            return respVO;
        }

        respVO.setRequestId(order.getRequestId());
        respVO.setOrderStatus(order.getStatus());
        respVO.setFailReason(order.getFailReason());
        if (CouponSeckillOrderStatusEnum.SUCCESS.getStatus().equals(order.getStatus())) {
            respVO.setResultCode(SeckillResultEnum.SUCCESS.getCode());
            respVO.setResultMsg(SeckillResultEnum.SUCCESS.getName());
        } else if (CouponSeckillOrderStatusEnum.FAIL.getStatus().equals(order.getStatus())) {
            respVO.setResultCode(SeckillResultEnum.FAILED.getCode());
            respVO.setResultMsg(order.getFailReason());
        } else {
            SeckillResultEnum cacheResult = activityCacheService.getSeckillResult(activityId, userId);
            respVO.setResultCode(cacheResult == null ? SeckillResultEnum.QUEUING.getCode() : cacheResult.getCode());
            respVO.setResultMsg(cacheResult == null ? SeckillResultEnum.QUEUING.getName() : cacheResult.getName());
        }
        return respVO;
    }

    private CouponActivityDO validateActivityCanSeckill(Long activityId) {
        CouponActivityDO activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_EXISTS, "优惠券活动不存在");
        }
        if (!CouponActivityStatusEnum.PUBLISHED.getStatus().equals(activity.getStatus())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_PUBLISHED, "优惠券活动未发布");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_STARTED, "活动未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_ENDED, "活动已结束");
        }
        if (activity.getAvailableStock() == null || activity.getAvailableStock() <= 0) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_STOCK_EMPTY, "优惠券已抢完");
        }
        return activity;
    }

    private void ensureRedisCacheReady(Long activityId) {
        if (activityCacheService.isActivityCached(activityId)) {
            return;
        }
        activityCacheService.preheatActivity(activityId);
    }

    private CouponSeckillLuaResultEnum tryLuaSeckillWithRetry(Long activityId, Long userId) {
        CouponSeckillLuaResultEnum result = activityCacheService.trySeckillByLua(activityId, userId);
        if (!CouponSeckillLuaResultEnum.CACHE_NOT_FOUND.equals(result)) {
            return result;
        }
        activityCacheService.preheatActivity(activityId);
        return activityCacheService.trySeckillByLua(activityId, userId);
    }

    private CouponSeckillOrderDO createQueuingOrder(Long userId, CouponActivityDO activity, String requestId) {
        CouponSeckillOrderDO order = new CouponSeckillOrderDO();
        order.setUserId(userId);
        order.setActivityId(activity.getId());
        order.setStoreId(activity.getStoreId());
        order.setRequestId(requestId);
        order.setStatus(CouponSeckillOrderStatusEnum.QUEUING.getStatus());
        orderMapper.insert(order);
        return order;
    }

    private CouponSeckillMessage buildMessage(Long userId, CouponActivityDO activity, CouponSeckillOrderDO order, String requestId) {
        CouponSeckillMessage message = new CouponSeckillMessage();
        message.setUserId(userId);
        message.setActivityId(activity.getId());
        message.setStoreId(activity.getStoreId());
        message.setOrderId(order.getId());
        message.setRequestId(requestId);
        message.setRequestTime(LocalDateTime.now());
        return message;
    }

    private CouponSeckillRespVO buildResp(String requestId, Long orderId, SeckillResultEnum resultEnum) {
        CouponSeckillRespVO respVO = new CouponSeckillRespVO();
        respVO.setRequestId(requestId);
        respVO.setOrderId(orderId);
        respVO.setResultCode(resultEnum.getCode());
        respVO.setResultMsg(resultEnum.getName());
        return respVO;
    }
}
