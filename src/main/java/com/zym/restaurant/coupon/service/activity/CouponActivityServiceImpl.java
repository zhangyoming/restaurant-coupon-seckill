package com.zym.restaurant.coupon.service.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.common.exception.BusinessException;
import com.zym.restaurant.coupon.common.exception.ErrorCodeConstants;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityPageReqVO;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivityRespVO;
import com.zym.restaurant.coupon.controller.admin.activity.vo.CouponActivitySaveReqVO;
import com.zym.restaurant.coupon.controller.app.activity.vo.AppCouponActivityRespVO;
import com.zym.restaurant.coupon.convert.CouponActivityConvert;
import com.zym.restaurant.coupon.dal.dataobject.CouponActivityDO;
import com.zym.restaurant.coupon.dal.mapper.CouponActivityMapper;
import com.zym.restaurant.coupon.enums.CouponActivityStatusEnum;
import com.zym.restaurant.coupon.framework.transaction.TransactionCallbackUtils;
import com.zym.restaurant.coupon.service.cache.CouponActivityCacheService;
import com.zym.restaurant.coupon.service.store.RestaurantStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponActivityServiceImpl implements CouponActivityService {

    private final CouponActivityMapper activityMapper;
    private final RestaurantStoreService storeService;
    private final CouponActivityCacheService activityCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(CouponActivitySaveReqVO reqVO) {
        validateActivityTime(reqVO);
        storeService.validateEnabledStore(reqVO.getStoreId());
        CouponActivityDO activity = CouponActivityConvert.toDO(reqVO);
        activityMapper.insert(activity);
        return activity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(CouponActivitySaveReqVO reqVO) {
        CouponActivityDO oldActivity = validateActivityExists(reqVO.getId());
        validateActivityTime(reqVO);
        storeService.validateEnabledStore(reqVO.getStoreId());
        CouponActivityDO updateObj = CouponActivityConvert.toDO(reqVO);
        updateObj.setStatus(null);
        updateObj.setAvailableStock(null); // 已创建活动不在普通修改里重置剩余库存
        activityMapper.updateById(updateObj);

        // 已发布活动被修改后，事务提交再刷新 Redis，避免缓存和数据库不一致。
        if (CouponActivityStatusEnum.PUBLISHED.getStatus().equals(oldActivity.getStatus())) {
            TransactionCallbackUtils.afterCommit(() -> activityCacheService.preheatActivity(reqVO.getId()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishActivity(Long id) {
        CouponActivityDO activity = validateActivityExists(id);
        CouponActivityDO updateObj = new CouponActivityDO();
        updateObj.setId(activity.getId());
        updateObj.setStatus(CouponActivityStatusEnum.PUBLISHED.getStatus());
        activityMapper.updateById(updateObj);

        // 发布成功后自动预热库存，方便活动开始时直接走 Redis 快速判断。
        TransactionCallbackUtils.afterCommit(() -> activityCacheService.preheatActivity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineActivity(Long id) {
        CouponActivityDO activity = validateActivityExists(id);
        CouponActivityDO updateObj = new CouponActivityDO();
        updateObj.setId(activity.getId());
        updateObj.setStatus(CouponActivityStatusEnum.OFFLINE.getStatus());
        activityMapper.updateById(updateObj);

        // 下架后清理 Redis，避免用户端继续通过缓存判断为可抢。
        TransactionCallbackUtils.afterCommit(() -> activityCacheService.evictActivity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long id) {
        validateActivityExists(id);
        activityMapper.deleteById(id);
        TransactionCallbackUtils.afterCommit(() -> activityCacheService.evictActivity(id));
    }

    @Override
    public void preheatActivity(Long id) {
        activityCacheService.preheatActivity(id);
    }

    @Override
    public int preheatPublishedActivities() {
        return activityCacheService.preheatPublishedActivities();
    }

    @Override
    public Integer getRedisStock(Long id) {
        return activityCacheService.getStock(id);
    }

    @Override
    public CouponActivityRespVO getActivity(Long id) {
        return CouponActivityConvert.toRespVO(activityMapper.selectById(id));
    }

    @Override
    public PageResult<CouponActivityRespVO> getActivityPage(CouponActivityPageReqVO reqVO) {
        Page<CouponActivityDO> page = activityMapper.selectPage(
                Page.of(reqVO.getPageNo(), reqVO.getPageSize()),
                buildPageQuery(reqVO).orderByDesc(CouponActivityDO::getId)
        );
        List<CouponActivityRespVO> list = page.getRecords().stream().map(CouponActivityConvert::toRespVO).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public PageResult<AppCouponActivityRespVO> getAppActivityPage(CouponActivityPageReqVO reqVO) {
        Page<CouponActivityDO> page = activityMapper.selectPage(
                Page.of(reqVO.getPageNo(), reqVO.getPageSize()),
                buildPageQuery(reqVO)
                        .eq(CouponActivityDO::getStatus, CouponActivityStatusEnum.PUBLISHED.getStatus())
                        .orderByDesc(CouponActivityDO::getId)
        );
        List<AppCouponActivityRespVO> list = page.getRecords().stream()
                .map(activity -> {
                    AppCouponActivityRespVO vo = CouponActivityConvert.toAppRespVO(activity);
                    Integer redisStock = activityCacheService.getStock(activity.getId());
                    if (redisStock != null) {
                        vo.setAvailableStock(redisStock);
                    }
                    return vo;
                })
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    private LambdaQueryWrapper<CouponActivityDO> buildPageQuery(CouponActivityPageReqVO reqVO) {
        return new LambdaQueryWrapper<CouponActivityDO>()
                .eq(reqVO.getStoreId() != null, CouponActivityDO::getStoreId, reqVO.getStoreId())
                .like(StringUtils.hasText(reqVO.getTitle()), CouponActivityDO::getTitle, reqVO.getTitle())
                .eq(reqVO.getStatus() != null, CouponActivityDO::getStatus, reqVO.getStatus());
    }

    private CouponActivityDO validateActivityExists(Long id) {
        CouponActivityDO activity = activityMapper.selectById(id);
        if (activity == null) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_NOT_EXISTS, "优惠券活动不存在");
        }
        return activity;
    }

    private void validateActivityTime(CouponActivitySaveReqVO reqVO) {
        if (!reqVO.getEndTime().isAfter(reqVO.getStartTime())) {
            throw BusinessException.of(ErrorCodeConstants.COUPON_ACTIVITY_TIME_INVALID, "活动结束时间必须晚于开始时间");
        }
    }
}
