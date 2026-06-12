package com.zym.restaurant.coupon.service.record;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordPageReqVO;
import com.zym.restaurant.coupon.controller.admin.record.vo.CouponRecordRespVO;
import com.zym.restaurant.coupon.convert.CouponRecordConvert;
import com.zym.restaurant.coupon.dal.dataobject.CouponRecordDO;
import com.zym.restaurant.coupon.dal.mapper.CouponRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponRecordServiceImpl implements CouponRecordService {

    private final CouponRecordMapper recordMapper;

    @Override
    public PageResult<CouponRecordRespVO> getRecordPage(CouponRecordPageReqVO reqVO) {
        return queryPage(reqVO, null);
    }

    @Override
    public PageResult<CouponRecordRespVO> getMyCouponPage(Long userId, CouponRecordPageReqVO reqVO) {
        return queryPage(reqVO, userId);
    }

    private PageResult<CouponRecordRespVO> queryPage(CouponRecordPageReqVO reqVO, Long fixedUserId) {
        Page<CouponRecordDO> page = recordMapper.selectPage(
                Page.of(reqVO.getPageNo(), reqVO.getPageSize()),
                new LambdaQueryWrapper<CouponRecordDO>()
                        .eq(fixedUserId != null, CouponRecordDO::getUserId, fixedUserId)
                        .eq(fixedUserId == null && reqVO.getUserId() != null, CouponRecordDO::getUserId, reqVO.getUserId())
                        .eq(reqVO.getActivityId() != null, CouponRecordDO::getActivityId, reqVO.getActivityId())
                        .eq(reqVO.getStoreId() != null, CouponRecordDO::getStoreId, reqVO.getStoreId())
                        .orderByDesc(CouponRecordDO::getId)
        );
        List<CouponRecordRespVO> list = page.getRecords().stream().map(CouponRecordConvert::toRespVO).toList();
        return new PageResult<>(list, page.getTotal());
    }
}
