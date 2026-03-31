package com.nexusfin.equity.thirdparty.qw;

import java.util.Map;
import org.springframework.http.HttpMethod;

public class AllinpayDirectSkeletonTransportMapper implements AllinpayDirectTransportMapper {

    @Override
    public AllinpayDirectTransportRequest map(AllinpayDirectPreparedRequest preparedRequest) {
        return new AllinpayDirectTransportRequest(
                preparedRequest.targetUri(),
                HttpMethod.POST,
                preparedRequest.contentType(),
                preparedRequest.requestBody(),
                Map.of(),
                Map.of("signature", preparedRequest.signature())
        );
    }
}
