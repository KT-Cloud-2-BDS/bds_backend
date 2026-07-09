package com.bds.member.service;

import com.bds.member.application.MemberService;
import com.bds.member.global.exception.BusinessException;
import com.bds.member.global.exception.ErrorCode;
import com.bds.member.infrastructure.persistence.adapter.MemberAdapter;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import com.bds.member.presentation.dto.AuthLoginRequestDto;
import com.bds.member.presentation.dto.MemberInfoRequestDto;
import com.bds.member.presentation.dto.MemberLoginRequestDto;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

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
    @DisplayName("회원가입 예외 핸들링")
    public class SignUpException {
        @Test
        @DisplayName("이미 존재하는 닉네임으로 가입 시 DUPLICATE_NICKNAME 예외가 터진다")
        public void 중복된_닉네임_가입_예외() {
            // given
            MemberSignupRequestDto requestDto = new MemberSignupRequestDto("test@email.com", "password123!", "중복닉네임");

            given(memberAdapter.existsByNickname(anyString())).willReturn(true);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.signUp(requestDto);
            });
            assertEquals(ErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
            verify(authFeignClient, never()).createAuthAccount(any());
        }
    }

    @Nested
    @DisplayName("로그인 예외 핸들링")
    public class LoginException {
        @Test
        @DisplayName("인증 서버로부터 2xx 상태 코드가 오지 않거나 바디가 비어있으면 AUTH_SERVICE_ERROR 예외가 터진다")
        public void 인증서버_통신실패_또는_바디비어있음_예외() {
            // given
            MemberLoginRequestDto requestDto = new MemberLoginRequestDto("test@email.com", "password123!");

            given(authFeignClient.login(any(AuthLoginRequestDto.class)))
                .willReturn(ResponseEntity.internalServerError().build());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.login(requestDto);
            });
            assertEquals(ErrorCode.AUTH_SERVICE_ERROR, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("닉네임 수정 예외 핸들링")
    public class UpdateNicknameException {
        @Test
        @DisplayName("수정할 닉네임이 null이거나 공백이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 닉네임_공백_예외() {
            // given
            Long authId = 24L;
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto(" ");

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.updateNickname(authId, requestDto);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }

        @Test
        @DisplayName("수정하려는 닉네임이 이미 사용 중이면 DUPLICATE_NICKNAME 예외가 터진다")
        public void 닉네임_중복_예외() {
            // given
            Long authId = 24L;
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto("이미있는닉네임");

            given(memberAdapter.existsByNickname(anyString())).willReturn(true);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.updateNickname(authId, requestDto);
            });
            assertEquals(ErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
        }

        @Test
        @DisplayName("회원 엔티티 정보가 조회되지 않으면 MEMBER_NOT_FOUND 예외가 터진다")
        public void 회원_정보없음_예외() {
            // given
            Long authId = 999L;
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto("새닉네임");

            given(memberAdapter.existsByNickname(anyString())).willReturn(false);
            given(memberAdapter.findByAuthId(anyLong())).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.updateNickname(authId, requestDto);
            });
            assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 예외 핸들링")
    public class DeleteMemberException {
        @Test
        @DisplayName("가입된 회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 터진다")
        public void 가입된회원_없음_예외발생() {
            // given
            Long authId = 999L;

            given(memberAdapter.existsByAuthId(anyLong())).willReturn(false);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.deleteMember(authId);
            });
            assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Auth 서버 통신이 실패(500)하면 AUTH_SERVICE_ERROR 예외가 터진다")
        public void 인증서버_통신실패_예외발생() {
            // given
            Long authId = 24L;

            given(memberAdapter.existsByAuthId(anyLong())).willReturn(true);
            given(authFeignClient.deleteAuth(anyLong())).willReturn(ResponseEntity.internalServerError().build());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.deleteMember(authId);
            });
            assertEquals(ErrorCode.AUTH_SERVICE_ERROR, exception.getErrorCode());
        }
    }
}