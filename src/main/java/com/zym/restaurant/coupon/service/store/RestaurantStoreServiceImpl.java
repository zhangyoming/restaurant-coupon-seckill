package com.zym.restaurant.coupon.service.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zym.restaurant.coupon.common.PageResult;
import com.zym.restaurant.coupon.common.exception.BusinessException;
import com.zym.restaurant.coupon.common.exception.ErrorCodeConstants;
import com.zym.restaurant.coupon.controller.admin.store.vo.StorePageReqVO;
import com.zym.restaurant.coupon.controller.admin.store.vo.StoreRespVO;
import com.zym.restaurant.coupon.controller.admin.store.vo.StoreSaveReqVO;
import com.zym.restaurant.coupon.convert.StoreConvert;
import com.zym.restaurant.coupon.dal.dataobject.RestaurantStoreDO;
import com.zym.restaurant.coupon.dal.mapper.RestaurantStoreMapper;
import com.zym.restaurant.coupon.enums.CommonStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantStoreServiceImpl implements RestaurantStoreService {

    private final RestaurantStoreMapper storeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStore(StoreSaveReqVO reqVO) {
        RestaurantStoreDO store = StoreConvert.toDO(reqVO);
        storeMapper.insert(store);
        return store.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStore(StoreSaveReqVO reqVO) {
        validateStoreExists(reqVO.getId());
        storeMapper.updateById(StoreConvert.toDO(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStore(Long id) {
        validateStoreExists(id);
        storeMapper.deleteById(id);
    }

    @Override
    public StoreRespVO getStore(Long id) {
        return StoreConvert.toRespVO(storeMapper.selectById(id));
    }

    @Override
    public PageResult<StoreRespVO> getStorePage(StorePageReqVO reqVO) {
        Page<RestaurantStoreDO> page = storeMapper.selectPage(
                Page.of(reqVO.getPageNo(), reqVO.getPageSize()),
                new LambdaQueryWrapper<RestaurantStoreDO>()
                        .like(StringUtils.hasText(reqVO.getName()), RestaurantStoreDO::getName, reqVO.getName())
                        .eq(reqVO.getStatus() != null, RestaurantStoreDO::getStatus, reqVO.getStatus())
                        .orderByDesc(RestaurantStoreDO::getId)
        );
        List<StoreRespVO> list = page.getRecords().stream().map(StoreConvert::toRespVO).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public void validateEnabledStore(Long id) {
        RestaurantStoreDO store = validateStoreExists(id);
        if (!CommonStatusEnum.ENABLE.getStatus().equals(store.getStatus())) {
            throw BusinessException.of(ErrorCodeConstants.STORE_DISABLED, "门店已禁用");
        }
    }

    private RestaurantStoreDO validateStoreExists(Long id) {
        RestaurantStoreDO store = storeMapper.selectById(id);
        if (store == null) {
            throw BusinessException.of(ErrorCodeConstants.STORE_NOT_EXISTS, "门店不存在");
        }
        return store;
    }
}
