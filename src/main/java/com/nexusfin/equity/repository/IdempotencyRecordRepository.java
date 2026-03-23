package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfin.equity.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyRecordRepository extends BaseMapper<IdempotencyRecord> {
}
