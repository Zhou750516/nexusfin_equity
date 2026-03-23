package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfin.equity.entity.BenefitOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BenefitOrderRepository extends BaseMapper<BenefitOrder> {
}
