package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.RegisterUserRequest;
import com.nexusfin.equity.dto.response.RegisterUserResponse;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.MemberOnboardingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberOnboardingServiceTest {

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private MemberChannelRepository memberChannelRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private MemberOnboardingServiceImpl memberOnboardingService;

    @Test
    void shouldReturnDuplicateWhenChannelAlreadyExists() {
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId("mem-1");
        when(memberChannelRepository.selectOne(any())).thenReturn(memberChannel);

        RegisterUserResponse response = memberOnboardingService.register(request("req-1", "user-1"));

        assertThat(response.memberId()).isEqualTo("mem-1");
        assertThat(response.registerStatus()).isEqualTo("DUPLICATE");
        verify(memberInfoRepository, never()).insert(any());
    }

    @Test
    void shouldCreateMemberChannelAndIdempotencyRecordForNewUser() {
        when(memberChannelRepository.selectOne(any())).thenReturn(null);
        when(idempotencyService.isProcessed("req-2")).thenReturn(false);
        when(memberInfoRepository.selectOne(any())).thenReturn(null);

        RegisterUserResponse response = memberOnboardingService.register(request("req-2", "user-2"));

        assertThat(response.registerStatus()).isEqualTo("SUCCESS");

        ArgumentCaptor<MemberInfo> memberCaptor = ArgumentCaptor.forClass(MemberInfo.class);
        ArgumentCaptor<MemberChannel> channelCaptor = ArgumentCaptor.forClass(MemberChannel.class);
        verify(memberInfoRepository).insert(memberCaptor.capture());
        verify(memberChannelRepository).insert(channelCaptor.capture());
        verify(idempotencyService).markProcessed("req-2", "REGISTER", memberCaptor.getValue().getMemberId(), memberCaptor.getValue().getMemberId());

        assertThat(memberCaptor.getValue().getExternalUserId()).isEqualTo("user-2");
        assertThat(memberCaptor.getValue().getMobileEncrypted()).isNotEqualTo("13800000000");
        assertThat(channelCaptor.getValue().getExternalUserId()).isEqualTo("user-2");
        assertThat(channelCaptor.getValue().getMemberId()).isEqualTo(memberCaptor.getValue().getMemberId());
    }

    @Test
    void shouldUseIdempotencyRecordWhenRequestAlreadyProcessedWithoutChannel() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setBizKey("mem-3");
        when(memberChannelRepository.selectOne(any())).thenReturn(null);
        when(idempotencyService.isProcessed("req-3")).thenReturn(true);
        when(idempotencyService.getByRequestId("req-3")).thenReturn(record);

        RegisterUserResponse response = memberOnboardingService.register(request("req-3", "user-3"));

        assertThat(response.memberId()).isEqualTo("mem-3");
        assertThat(response.registerStatus()).isEqualTo("DUPLICATE");
    }

    private RegisterUserRequest request(String requestId, String externalUserId) {
        return new RegisterUserRequest(
                requestId,
                "KJ",
                new RegisterUserRequest.UserInfo(externalUserId, "13800000000", "310101199001011234", "张三")
        );
    }
}
