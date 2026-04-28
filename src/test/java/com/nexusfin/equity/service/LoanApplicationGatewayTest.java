package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.impl.LoanApplicationGatewayImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationGatewayTest {

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    private LoanApplicationGateway loanApplicationGateway;

    @BeforeEach
    void setUp() {
        loanApplicationGateway = new LoanApplicationGatewayImpl(loanApplicationMappingRepository);
    }

    @Test
    void shouldSaveActiveMappingWithCurrentDefaults() {
        loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
                "mem-001",
                "user-001",
                "APP-001",
                "BEN-001",
                "LN-001",
                "rent",
                "ACTIVE"
        ));

        ArgumentCaptor<LoanApplicationMapping> captor = ArgumentCaptor.forClass(LoanApplicationMapping.class);
        verify(loanApplicationMappingRepository).insert(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo("APP-001");
        assertThat(captor.getValue().getMemberId()).isEqualTo("mem-001");
        assertThat(captor.getValue().getBenefitOrderNo()).isEqualTo("BEN-001");
        assertThat(captor.getValue().getChannelCode()).isEqualTo("KJ");
        assertThat(captor.getValue().getExternalUserId()).isEqualTo("user-001");
        assertThat(captor.getValue().getUpstreamQueryType()).isEqualTo("loanId");
        assertThat(captor.getValue().getUpstreamQueryValue()).isEqualTo("LN-001");
        assertThat(captor.getValue().getPurpose()).isEqualTo("rent");
        assertThat(captor.getValue().getMappingStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getCreatedTs()).isNotNull();
        assertThat(captor.getValue().getUpdatedTs()).isNotNull();
    }

    @Test
    void shouldSavePendingReviewMappingWithCurrentDefaults() {
        loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
                "mem-001",
                "user-001",
                "APP-002",
                "BEN-002",
                "LN-002",
                "education",
                "PENDING_REVIEW"
        ));

        ArgumentCaptor<LoanApplicationMapping> captor = ArgumentCaptor.forClass(LoanApplicationMapping.class);
        verify(loanApplicationMappingRepository).insert(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo("APP-002");
        assertThat(captor.getValue().getMappingStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(captor.getValue().getPurpose()).isEqualTo("education");
    }

    @Test
    void shouldFindActiveOrPendingMappingByMemberIdAndApplicationId() {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-003");
        mapping.setMemberId("mem-001");
        mapping.setUpstreamQueryValue("LN-003");
        mapping.setPurpose("rent");
        mapping.setMappingStatus("ACTIVE");
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(mapping);

        LoanApplicationMapping result =
                loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-003");

        assertThat(result).isSameAs(mapping);
        verify(loanApplicationMappingRepository).selectOne(any());
    }

    @Test
    void shouldReturnNullWhenActiveOrPendingMappingDoesNotExist() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        LoanApplicationMapping result =
                loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-404");

        assertThat(result).isNull();
        verify(loanApplicationMappingRepository).selectOne(any());
    }
}
