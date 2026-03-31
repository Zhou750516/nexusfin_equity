package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayDirectResponseParserTest {

    @Test
    void shouldReportProtocolUnimplementedWhenParsingResponse() {
        AllinpayDirectResponseParser parser = new AllinpayDirectSkeletonResponseParser();

        assertThatThrownBy(() -> parser.parse(
                AllinpayDirectOperation.MEMBER_SYNC,
                "SYNC001",
                new AllinpayDirectVerifiedResponse(200, "<xml>todo</xml>", "resp-signature"),
                QwMemberSyncResponse.class
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED")
                .hasMessageContaining("MEMBER_SYNC")
                .hasMessageContaining("SYNC001");
    }
}
