package com.nexusfin.equity.support;

import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Specialized base for ITs that mock both the Yunka HTTP gateway and the Xiaohua gateway service.
 * Used by Phase9TaskGroupC/D (and candidates for migration: Phase9TaskGroupE, Repayment, Loan, Benefits).
 *
 * Sharing the @MockBean set across these ITs collapses their Spring TestContext cache keys,
 * so a single ApplicationContext is reused instead of one per IT.
 */
public abstract class AbstractYunkaXiaohuaIT extends AbstractIntegrationTest {

    @MockBean
    protected YunkaGatewayClient yunkaGatewayClient;

    @MockBean
    protected XiaohuaGatewayService xiaohuaGatewayService;
}
