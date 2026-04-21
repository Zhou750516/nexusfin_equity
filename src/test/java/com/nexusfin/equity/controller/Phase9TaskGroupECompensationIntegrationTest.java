package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.repository.AsyncCompensationAttemptRepository;
import com.nexusfin.equity.repository.AsyncCompensationPartitionRuntimeRepository;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase9TaskGroupECompensationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenefitProductRepository benefitProductRepository;

    @Autowired
    private BenefitOrderRepository benefitOrderRepository;

    @Autowired
    private MemberInfoRepository memberInfoRepository;

    @Autowired
    private MemberChannelRepository memberChannelRepository;

    @Autowired
    private MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    @Autowired
    private SignTaskRepository signTaskRepository;

    @Autowired
    private ContractArchiveRepository contractArchiveRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Autowired
    private AsyncCompensationTaskRepository asyncCompensationTaskRepository;

    @Autowired
    private AsyncCompensationAttemptRepository asyncCompensationAttemptRepository;

    @Autowired
    private AsyncCompensationPartitionRuntimeRepository asyncCompensationPartitionRuntimeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SensitiveDataCipher sensitiveDataCipher;

    @MockBean
    private QwBenefitClient qwBenefitClient;

    @BeforeEach
    void setUp() {
        contractArchiveRepository.delete(null);
        signTaskRepository.delete(null);
        asyncCompensationAttemptRepository.delete(null);
        asyncCompensationPartitionRuntimeRepository.delete(null);
        asyncCompensationTaskRepository.delete(null);
        benefitOrderRepository.delete(null);
        idempotencyRecordRepository.delete(null);
        memberPaymentProtocolRepository.delete(null);
        memberChannelRepository.delete(null);
        loanApplicationMappingRepository.delete(null);
        memberInfoRepository.delete(null);
        benefitProductRepository.delete(null);
    }

    @Test
    void shouldPersistBenefitOrderAndCompensationTaskWhenBenefitPurchaseTimesOutDuringLoanApply() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-benefit-timeout", "user-benefit-timeout");
        createProduct("HUXUAN_CARD");
        when(qwBenefitClient.syncMemberOrder(any()))
                .thenThrow(new UpstreamTimeoutException("QW member sync timeout"));

        mockMvc.perform(post("/api/loan/apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 3000,
                                  "term": 3,
                                  "receivingAccountId": "acc_001",
                                  "agreedProtocols": ["user_agreement", "loan_agreement", "privacy_policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("QW_SYNC_TIMEOUT")));

        verify(qwBenefitClient).syncMemberOrder(any());

        BenefitOrder benefitOrder = benefitOrderRepository.selectOne(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getMemberId, memberInfo.getMemberId())
                .last("limit 1"));
        assertThat(benefitOrder).isNotNull();
        assertThat(benefitOrder.getSyncStatus()).isEqualTo("SYNC_FAIL");

        AsyncCompensationTask task = asyncCompensationTaskRepository.selectOne(Wrappers.<AsyncCompensationTask>lambdaQuery()
                .eq(AsyncCompensationTask::getTaskType, "QW_BENEFIT_PURCHASE_RETRY")
                .eq(AsyncCompensationTask::getBizOrderNo, benefitOrder.getBenefitOrderNo())
                .last("limit 1"));
        assertThat(task).isNotNull();
        assertThat(task.getTaskStatus()).isEqualTo("INIT");
        assertThat(task.getBizKey()).isEqualTo("BENEFIT_PURCHASE:" + benefitOrder.getBenefitOrderNo());
        assertThat(asyncCompensationAttemptRepository.selectCount(null)).isZero();
        assertThat(asyncCompensationPartitionRuntimeRepository.selectCount(null)).isZero();

        LoanApplicationMapping mapping = loanApplicationMappingRepository.selectOne(Wrappers.<LoanApplicationMapping>lambdaQuery()
                .eq(LoanApplicationMapping::getMemberId, memberInfo.getMemberId())
                .last("limit 1"));
        assertThat(mapping).isNull();
    }

    private BenefitProduct createProduct(String productCode) {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode(productCode);
        product.setProductName("惠选卡");
        product.setFeeRate(300);
        product.setStatus("ACTIVE");
        product.setCreatedTs(LocalDateTime.now());
        product.setUpdatedTs(LocalDateTime.now());
        benefitProductRepository.insert(product);
        return product;
    }

    private MemberInfo createReadyMember(String memberId, String externalUserId) {
        MemberInfo memberInfo = createMember(memberId, externalUserId);
        createChannel(memberId, externalUserId);
        createActiveProtocol(memberId, externalUserId);
        return memberInfo;
    }

    private MemberInfo createMember(String memberId, String externalUserId) {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId(memberId);
        memberInfo.setTechPlatformUserId(externalUserId);
        memberInfo.setExternalUserId(externalUserId);
        memberInfo.setMobileEncrypted(sensitiveDataCipher.encrypt("13800138000"));
        memberInfo.setMobileHash("hash-mobile-" + UUID.randomUUID());
        memberInfo.setIdCardEncrypted(sensitiveDataCipher.encrypt("110101199003071234"));
        memberInfo.setIdCardHash("hash-id-" + UUID.randomUUID());
        memberInfo.setRealNameEncrypted(sensitiveDataCipher.encrypt("测试用户"));
        memberInfo.setMemberStatus(MemberStatusEnum.ACTIVE.name());
        memberInfo.setCreatedTs(LocalDateTime.now());
        memberInfo.setUpdatedTs(LocalDateTime.now());
        memberInfoRepository.insert(memberInfo);
        return memberInfo;
    }

    private void createChannel(String memberId, String externalUserId) {
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId(memberId);
        memberChannel.setChannelCode("KJ");
        memberChannel.setExternalUserId(externalUserId);
        memberChannel.setBindStatus("BOUND");
        memberChannel.setCreatedTs(LocalDateTime.now());
        memberChannel.setUpdatedTs(LocalDateTime.now());
        memberChannelRepository.insert(memberChannel);
    }

    private void createActiveProtocol(String memberId, String externalUserId) {
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setMemberId(memberId);
        protocol.setExternalUserId(externalUserId);
        protocol.setProviderCode("ALLINPAY");
        protocol.setProtocolNo("AIP-TEST-" + memberId);
        protocol.setProtocolStatus("ACTIVE");
        protocol.setChannelCode("KJ");
        protocol.setSignedTs(LocalDateTime.now());
        protocol.setLastVerifiedTs(LocalDateTime.now());
        protocol.setCreatedTs(LocalDateTime.now());
        protocol.setUpdatedTs(LocalDateTime.now());
        memberPaymentProtocolRepository.insert(protocol);
    }

    private Cookie authCookie(MemberInfo memberInfo) {
        return new Cookie(
                "NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId())
        );
    }
}
