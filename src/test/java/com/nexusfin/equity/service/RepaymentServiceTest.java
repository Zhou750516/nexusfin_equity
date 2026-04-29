package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.RepaymentSmsConfirmRequest;
import com.nexusfin.equity.dto.request.RepaymentSmsSendRequest;
import com.nexusfin.equity.dto.response.RepaymentInfoResponse;
import com.nexusfin.equity.dto.response.RepaymentResultResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsConfirmResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsSendResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.RepaymentServiceImpl;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmResponse;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.SensitiveDataCipher;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Mock
    private SensitiveDataCipher sensitiveDataCipher;

    private RepaymentService repaymentService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        repaymentService = new RepaymentServiceImpl(
                h5LoanProperties(),
                yunkaProperties(),
                yunkaGatewayClient,
                h5I18nService,
                xiaohuaGatewayService,
                memberInfoRepository,
                loanApplicationMappingRepository,
                sensitiveDataCipher,
                new YunkaCallTemplate(yunkaGatewayClient)
        );
    }

    @Test
    void shouldReturnBoundCardsAndDefaultCardInRepaymentInfo() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("user-001", "LN-001"));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {"repayAmount":101850}
                        """)
        ));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("LN-001"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1),
                        new UserCardSummary("card-002", "建设银行", "1234", 0)
                )));

        RepaymentInfoResponse response = repaymentService.getInfo("user-001", "LN-001");

        assertThat(response.bankCard().bankName()).isEqualTo("招商银行");
        assertThat(response.bankCard().accountId()).isEqualTo("card-001");
        assertThat(response.bankCards()).hasSize(2);
        assertThat(response.smsRequired()).isTrue();
    }

    @Test
    void shouldRejectUnknownLoanIdBeforeCallingYunka() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> repaymentService.getInfo("user-001", "LN-FAKE-001"))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getCode(),
                        throwable -> ((BizException) throwable).getErrorMsg())
                .containsExactly(404, "repayment loan reference not found");

        verifyNoInteractions(yunkaGatewayClient);
        verify(xiaohuaGatewayService, never()).queryUserCards(any(), any(), any());
    }

    @Test
    void shouldSendRepaymentSmsUsingSelectedCardAndMemberProfile() {
        when(xiaohuaGatewayService.queryUserCards(any(), eq("LN-002"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(memberInfoRepository.selectByTechPlatformUserId("user-001")).thenReturn(memberInfo());
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(xiaohuaGatewayService.sendCardSms(any(), eq("LN-002"), any()))
                .thenReturn(new CardSmsSendResponse("sms-001", "11001", "发送成功"));

        RepaymentSmsSendResponse response = repaymentService.sendSms(
                "user-001",
                new RepaymentSmsSendRequest("LN-002", "card-001")
        );

        assertThat(response.smsSeq()).isEqualTo("sms-001");
        assertThat(response.status()).isEqualTo("sent");
        ArgumentCaptor<com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest> captor =
                ArgumentCaptor.forClass(com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest.class);
        verify(xiaohuaGatewayService).sendCardSms(any(), eq("LN-002"), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("user-001");
        assertThat(captor.getValue().bankCardNum()).isEqualTo("card-001");
        assertThat(captor.getValue().type()).isEqualTo(2);
        assertThat(captor.getValue().phone()).isEqualTo("13800138000");
    }

    @Test
    void shouldConfirmRepaymentSmsUsingLatestGatewayFields() {
        when(memberInfoRepository.selectByTechPlatformUserId("user-001")).thenReturn(memberInfo());
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(xiaohuaGatewayService.confirmCardSms(any(), eq("LN-003"), any()))
                .thenReturn(new CardSmsConfirmResponse("11002", "验证成功"));

        RepaymentSmsConfirmResponse response = repaymentService.confirmSms(
                "user-001",
                new RepaymentSmsConfirmRequest("LN-003", "123456")
        );

        assertThat(response.status()).isEqualTo("confirmed");
    }

    @Test
    void shouldMapLatestRepaymentQueryStatuses() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("user-001", "LN-001"));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "status": "8004",
                          "amount": 101850,
                          "swiftNumber": "RP-LN-001",
                          "discount": 2650,
                          "bankCardNum": "6222020202028648"
                        }
                        """)
        ));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("RP-LN-001"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("6222020202028648", "招商银行", "8648", 1)
                )));

        RepaymentResultResponse response = repaymentService.getResult("user-001", "RP-LN-001");

        assertThat(response.status()).isEqualTo("processing");
        assertThat(response.swiftNumber()).isEqualTo("RP-LN-001");
        assertThat(response.interestSaved()).isEqualByComparingTo("26.50");
        assertThat(response.bankCard().lastFour()).isEqualTo("8648");
    }

    @Test
    void shouldRejectUnknownRepaymentIdBeforeCallingYunka() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> repaymentService.getResult("user-001", "RP-LN-FAKE-001"))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getCode(),
                        throwable -> ((BizException) throwable).getErrorMsg())
                .containsExactly(404, "repayment reference not found");

        verifyNoInteractions(yunkaGatewayClient);
        verify(xiaohuaGatewayService, never()).queryUserCards(any(), any(), any());
    }

    private LoanApplicationMapping loanMapping(String externalUserId, String loanId) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-" + loanId);
        mapping.setExternalUserId(externalUserId);
        mapping.setUpstreamQueryValue(loanId);
        mapping.setMappingStatus("ACTIVE");
        return mapping;
    }

    private MemberInfo memberInfo() {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setTechPlatformUserId("user-001");
        memberInfo.setMobileEncrypted("mobile-cipher");
        memberInfo.setIdCardEncrypted("id-cipher");
        memberInfo.setRealNameEncrypted("name-cipher");
        return memberInfo;
    }

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(new H5LoanProperties.TermOption("3期", 3)),
                BigDecimal.valueOf(0.18),
                "XX商业银行",
                new H5LoanProperties.ReceivingAccount("招商银行", "8648", "acc_001")
        );
    }

    private YunkaProperties yunkaProperties() {
        return new YunkaProperties(
                true,
                "REST",
                "http://localhost:8080",
                "/api/gateway/proxy",
                3000,
                5000,
                new YunkaProperties.Paths(
                        "/loan/trail",
                        "/loan/query",
                        "/loan/apply",
                        "/repay/trial",
                        "/repay/apply",
                        "/repay/query",
                        "/protocol/queryProtocolAggregationLink",
                        "/user/token",
                        "/user/query",
                        "/loan/repayPlan",
                        "/card/smsSend",
                        "/card/smsConfirm",
                        "/card/userCards",
                        "/credit/image/query",
                        "/benefit/sync"
                )
        );
    }
}
