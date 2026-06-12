package com.zym.restaurant.coupon.service.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zym.restaurant.coupon.dal.dataobject.CouponActivityDO;
import com.zym.restaurant.coupon.dal.dataobject.CouponRecordDO;
import com.zym.restaurant.coupon.dal.dataobject.CouponSeckillOrderDO;
import com.zym.restaurant.coupon.dal.mapper.CouponActivityMapper;
import com.zym.restaurant.coupon.dal.mapper.CouponRecordMapper;
import com.zym.restaurant.coupon.dal.mapper.CouponSeckillOrderMapper;
import com.zym.restaurant.coupon.enums.CouponRecordStatusEnum;
import com.zym.restaurant.coupon.enums.CouponSeckillOrderStatusEnum;
import com.zym.restaurant.coupon.enums.SeckillResultEnum;
import com.zym.restaurant.coupon.mq.message.CouponSeckillMessage;
import com.zym.restaurant.coupon.service.cache.CouponActivityCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * RabbitMQ 异步发券落库处理。
 *
 * 幂等设计：
 * 1. 先按 orderId / requestId 查询秒杀订单；
 * 2. 只有“排队中”的订单才允许继续处理；
 * 3. coupon_record 表保留 user_id + activity_id 唯一索引，防止重复发券；
 * 4. coupon_activity 扣库存使用 available_stock > 0 条件更新，防止数据库层超卖；
 * 5. 数据库落库失败时补偿 Redis Lua 预扣库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponSeckillOrderProcessServiceImpl implements CouponSeckillOrderProcessService {

    private final CouponActivityMapper activityMapper;
    private final CouponRecordMapper recordMapper;
    private final CouponSeckillOrderMapper orderMapper;
    private final CouponActivityCacheService activityCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processSeckillMessage(CouponSeckillMessage message) {
        CouponSeckillOrderDO order = getOrder(message);
        if (order == null) {
            log.warn("秒杀订单不存在，忽略消息：{}", message);
            return;
        }
        if (!CouponSeckillOrderStatusEnum.QUEUING.getStatus().equals(order.getStatus())) {
            log.info("秒杀订单已处理，忽略重复消息，orderId={}, requestId={}, status={}",
                    order.getId(), order.getRequestId(), order.getStatus());
            return;
        }

        CouponActivityDO activity = activityMapper.selectById(message.getActivityId());
        if (activity == null) {
            failAndCompensate(order, message, "优惠券活动不存在");
            return;
        }

        int updated = activityMapper.deductStockIfAvailable(activity.getId());
        if (updated == 0) {
            // Redis 预扣成功但数据库库存不足，说明缓存与数据库不一致，必须补偿本次 Redis 预扣。
            activityCacheService.markSoldOut(activity.getId());
            failAndCompensate(order, message, "优惠券已抢完");
            return;
        }

        try {
            CouponRecordDO record = buildCouponRecord(message.getUserId(), activity);
            recordMapper.insert(record);
        } catch (DuplicateKeyException duplicateKeyException) {
            // 数据库唯一索引兜底，防止 MQ 重试或缓存异常导致一人多券。
            activityMapper.restoreStock(activity.getId());
            failAndCompensate(order, message, "请勿重复领取该优惠券");
            return;
        } catch (RuntimeException ex) {
            activityMapper.restoreStock(activity.getId());
            activityCacheService.compensateSeckillReservation(message.getActivityId(), message.getUserId());
            throw ex;
        }

        CouponSeckillOrderDO updateObj = new CouponSeckillOrderDO();
        updateObj.setId(order.getId());
        updateObj.setStatus(CouponSeckillOrderStatusEnum.SUCCESS.getStatus());
        updateObj.setFailReason(null);
        orderMapper.updateById(updateObj);
        activityCacheService.setSeckillResult(message.getActivityId(), message.getUserId(), SeckillResultEnum.SUCCESS);
    }

    private CouponSeckillOrderDO getOrder(CouponSeckillMessage message) {
        if (message.getOrderId() != null) {
            CouponSeckillOrderDO order = orderMapper.selectById(message.getOrderId());
            if (order != null) {
                return order;
            }
        }
        return orderMapper.selectOne(new LambdaQueryWrapper<CouponSeckillOrderDO>()
                .eq(CouponSeckillOrderDO::getRequestId, message.getRequestId())
                .last("LIMIT 1"));
    }

    private CouponRecordDO buildCouponRecord(Long userId, CouponActivityDO activity) {
        CouponRecordDO record = new CouponRecordDO();
        record.setUserId(userId);
        record.setActivityId(activity.getId());
        record.setStoreId(activity.getStoreId());
        record.setCouponName(activity.getCouponName());
        record.setCouponAmount(activity.getCouponAmount());
        record.setThresholdAmount(activity.getThresholdAmount());
        record.setStatus(CouponRecordStatusEnum.UNUSED.getStatus());
        record.setReceiveTime(LocalDateTime.now());
        return record;
    }

    private void failAndCompensate(CouponSeckillOrderDO order, CouponSeckillMessage message, String failReason) {
        activityCacheService.compensateSeckillReservation(message.getActivityId(), message.getUserId());

        CouponSeckillOrderDO updateObj = new CouponSeckillOrderDO();
        updateObj.setId(order.getId());
        updateObj.setStatus(CouponSeckillOrderStatusEnum.FAIL.getStatus());
        updateObj.setFailReason(failReason);
        orderMapper.updateById(updateObj);
        activityCacheService.setSeckillResult(message.getActivityId(), message.getUserId(), SeckillResultEnum.FAILED);
    }
}
