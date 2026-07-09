package com.bds.member.service;

import com.bds.member.application.MemberService; // 오리지널 MemberService 경로에 맞게 자동 임포트 확인!
import com.bds.member.infrastructure.persistence.adapter.MemberAdapter;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트 - 성공 케이스")
public class MemberServiceUnitTest {

    @InjectMocks
    public MemberService memberService;

    @Mock
    public MemberAdapter memberAdapter;

    @Mock
    public AuthFeignClient authFeignClient;

    @Nested
    @DisplayName("회원 탈퇴 기능")
    public class DeleteMember {

        @Test
        @DisplayName("유저가 존재하고 Auth 서버 통신이 성공하면 소프트 딜리트가 정상 수행된다")
        public void 유저존재_통신성공_소프트딜리트수행() {
            // given
            Long authId = 24L;
            given(memberAdapter.existsByAuthId(authId)).willReturn(true);
            given(authFeignClient.deleteAuth(authId)).willReturn(ResponseEntity.ok().build());

            // when
            memberService.deleteMember(authId);

            // then
            verify(memberAdapter, times(1)).softDeleteByAuthId(authId);
        }
    }
}