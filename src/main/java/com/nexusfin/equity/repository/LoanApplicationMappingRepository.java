package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoanApplicationMappingRepository extends BaseMapper<LoanApplicationMapping> {
}
