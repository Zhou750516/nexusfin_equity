package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationReceiveLogRepository extends BaseMapper<NotificationReceiveLog> {

    default List<NotificationReceiveLog> selectByBenefitOrderNo(String benefitOrderNo) {
        return this.selectList(Wrappers.<NotificationReceiveLog>lambdaQuery()
                .eq(NotificationReceiveLog::getBenefitOrderNo, benefitOrderNo)
                .orderByAsc(NotificationReceiveLog::getReceivedTs));
    }

    default List<NotificationReceiveLog> selectByRequestId(String requestId) {
        return this.selectList(Wrappers.<NotificationReceiveLog>lambdaQuery()
                .eq(NotificationReceiveLog::getRequestId, requestId)
                .orderByAsc(NotificationReceiveLog::getReceivedTs));
    }
}
