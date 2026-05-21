package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.LoanApplicationGateway;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class LoanApplicationGatewayImpl implements LoanApplicationGateway {

    private static final String DEFAULT_CHANNEL_CODE = "KJ";
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;

    public LoanApplicationGatewayImpl(LoanApplicationMappingRepository loanApplicationMappingRepository) {
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
    }

    @Override
    public void save(SaveCommand command) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(command.applicationId());
        mapping.setMemberId(command.memberId());
        mapping.setBenefitOrderNo(command.benefitOrderNo());
        mapping.setChannelCode(DEFAULT_CHANNEL_CODE);
        mapping.setExternalUserId(command.externalUserId());
        mapping.setPlatformLoanId(command.platformLoanId());
        mapping.setPurpose(command.purpose());
        mapping.setMappingStatus(command.mappingStatus());
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        loanApplicationMappingRepository.insert(mapping);
    }

    @Override
    public LoanApplicationMapping findLatestPendingMapping(String memberId) {
        return loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getMemberId, memberId)
                        .eq(LoanApplicationMapping::getMappingStatus, "PENDING_REVIEW")
                        .orderByDesc(LoanApplicationMapping::getCreatedTs)
                        .last("limit 1")
        );
    }

    @Override
    public LoanApplicationMapping findActiveOrPendingMapping(String memberId, String applicationId) {
        return loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getApplicationId, applicationId)
                        .eq(LoanApplicationMapping::getMemberId, memberId)
                        .in(LoanApplicationMapping::getMappingStatus, "ACTIVE", "PENDING_REVIEW")
                        .last("limit 1")
        );
    }
}
