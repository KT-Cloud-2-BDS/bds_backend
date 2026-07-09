package com.bds.member.service;

import com.bds.member.application.MemberService;
import com.bds.member.infrastructure.persistence.adapter.MemberAdapter;
import com.bds.member.global.exception.BusinessException;
import com.bds.member.global.exception.ErrorCode;
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
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트 - 예외 케이스")
public class MemberServiceUnitExceptionTest {

    @InjectMocks
    public MemberService memberService;

    @Mock
    public MemberAdapter memberAdapter;

    @Mock
    public AuthFeignClient authFeignClient;

    @Nested
    @DisplayName("회원 탈퇴 예외 핸들링")
    public class DeleteMemberException {

        @Test
        @DisplayName("가입된 회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 터진다")
        public void 가입된회원_없음_예외발생() {
            // given
            Long authId = 999L;
            given(memberAdapter.existsByAuthId(authId)).willReturn(false);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.deleteMember(authId);
            });

            assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
            verify(authFeignClient, never()).deleteAuth(anyLong());
            verify(memberAdapter, never()).softDeleteByAuthId(anyLong());
        }

        @Test
        @DisplayName("Auth 서버 통신이 실패하면 AUTH_SERVICE_ERROR 예외가 터지고 내부 DB는 변경되지 않는다")
        public void 인증서버_통신실패_예외발생() {
            // given
            Long authId = 24L;
            given(memberAdapter.existsByAuthId(authId)).willReturn(true);
            given(authFeignClient.deleteAuth(authId)).willReturn(ResponseEntity.internalServerError().build());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.deleteMember(authId);
            });

            assertEquals(ErrorCode.AUTH_SERVICE_ERROR, exception.getErrorCode());
            verify(memberAdapter, never()).softDeleteByAuthId(anyLong());
        }
    }
}