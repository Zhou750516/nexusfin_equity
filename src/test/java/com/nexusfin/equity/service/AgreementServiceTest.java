package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.SignTaskResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.ContractArchive;
import com.nexusfin.equity.entity.SignTask;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.service.impl.AgreementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgreementServiceTest {

    @Mock
    private SignTaskRepository signTaskRepository;

    @Mock
    private ContractArchiveRepository contractArchiveRepository;

    @InjectMocks
    private AgreementServiceImpl agreementService;

    @Test
    void shouldGenerateSignTasksAndArchivesForMissingArtifacts() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-1");
        when(signTaskRepository.selectOne(any())).thenReturn(null);
        when(contractArchiveRepository.selectOne(any())).thenReturn(null);

        SignTaskResponse response = agreementService.ensureAgreementArtifacts(order);

        assertThat(response.benefitOrderNo()).isEqualTo("ord-1");
        assertThat(response.tasks()).hasSize(2);
        verify(signTaskRepository, times(2)).insert(any(SignTask.class));
        verify(contractArchiveRepository, times(2)).insert(any(ContractArchive.class));
    }

    @Test
    void shouldReuseExistingSignTaskAndArchive() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-2");
        SignTask signTask = new SignTask();
        signTask.setTaskNo("tsk-1");
        signTask.setContractType("EQUITY_AGREEMENT");
        signTask.setSignStatus("SIGNED");
        signTask.setSignUrl("https://abs.example.com/sign");
        ContractArchive archive = new ContractArchive();
        archive.setTaskNo("tsk-1");
        when(signTaskRepository.selectOne(any())).thenReturn(signTask);
        when(contractArchiveRepository.selectOne(any())).thenReturn(archive);

        agreementService.ensureAgreementArtifacts(order);

        verify(signTaskRepository, times(0)).insert(any(SignTask.class));
        verify(contractArchiveRepository, times(0)).insert(any(ContractArchive.class));
    }
}
