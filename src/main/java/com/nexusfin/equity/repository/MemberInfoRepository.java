package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfin.equity.entity.MemberInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberInfoRepository extends BaseMapper<MemberInfo> {
}
