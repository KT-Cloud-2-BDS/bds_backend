package com.bds.auth.service;

import com.bds.auth.application.AuthService;
import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.domain.repository.AuthRepository;
import com.bds.auth.domain.repository.AuthLocalRepository;
import com.bds.auth.domain.repository.TokenCacheRepository;
import com.bds.auth.global.exception.BusinessException;
import com.bds.auth.global.exception.ErrorCode;
import com.bds.auth.infrastructure.security.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트 - 예외 케이스")
public class AuthServiceUnitExceptionTest {

    @InjectMocks
    public AuthService authService;

    @Mock
    public AuthRepository authRepository;

    @Mock
    public AuthLocalRepository authLocalRepository;

    @Mock
    public TokenCacheRepository tokenCacheRepository;

    @Mock
    public PasswordEncoder passwordEncoder;

    @Mock
    public JwtTokenUtil jwtTokenUtil;

    @Nested
    @DisplayName("회원가입 인증 코드 발송 예외")
    public class SendSignUpVerificationCodeException {
        @Test
        @DisplayName("이미 가입된 이메일 주소라면 DUPLICATE_EMAIL 예외가 터진다")
        public void 이메일_중복_예외() {
            // given
            String email = "duplicate@email.com";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            given(authRepository.findByEmail(anyString())).willReturn(Optional.of(mockAuth));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.sendSignUpVerificationCode(email);
            });
            assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    public class VerifyCodeException {
        @Test
        @DisplayName("Redis에 인증 코드가 만료되어 존재하지 않으면 VERIFICATION_CODE_EXPIRED 예외가 터진다")
        public void 인증코드_만료_예외() {
            // given
            String email = "yeojin@email.com";
            given(tokenCacheRepository.get(anyString())).willReturn(null);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.verifyCode(email, "123456");
            });
            assertEquals(ErrorCode.VERIFICATION_CODE_EXPIRED, exception.getErrorCode());
        }

        @Test
        @DisplayName("Redis에 저장된 코드와 입력받은 코드가 일치하지 않으면 VERIFICATION_CODE_MISMATCH 예외가 터진다")
        public void 인증코드_불일치_예외() {
            // given
            String email = "yeojin@email.com";
            given(tokenCacheRepository.get("verify:" + email)).willReturn("123456");

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.verifyCode(email, "999999");
            });
            assertEquals(ErrorCode.VERIFICATION_CODE_MISMATCH, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("계정 생성 예외")
    public class CreateAccountException {
        @Test
        @DisplayName("이메일 인증 성공 티켓이 true가 아니라면 UNVERIFIED_EMAIL 예외가 터진다")
        public void 이메일_미인증_예외() {
            // given
            String email = "yeojin@email.com";
            given(tokenCacheRepository.get("verified:" + email)).willReturn(null);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.createAccount(email, "password123!");
            });
            assertEquals(ErrorCode.UNVERIFIED_EMAIL, exception.getErrorCode());
        }

        @Test
        @DisplayName("인증은 완료되었으나 가입 직전 이메일이 중복되면 DUPLICATE_EMAIL 예외가 터진다")
        public void 인증후_가입시점_이메일_중복_예외() {
            // given
            String email = "yeojin@email.com";
            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            given(tokenCacheRepository.get("verified:" + email)).willReturn("true");

            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.createAccount(email, "password123!");
            });
            assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("로그인 예외")
    public class LoginException {
        @Test
        @DisplayName("존재하지 않는 이메일 주소라면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 계정_존재하지않음_이메일오류_예외() {
            // given
            String email = "wrong@email.com";
            given(authRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, "password123!");
            });
            assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("계정은 존재하나 ACTIVE 상태가 아니라면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 비활성화_계정_로그인_차단_예외() {
            // given
            String email = "yeojin@email.com";
            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getStatus()).willReturn(Status.DELETED);

            given(authRepository.findByEmail(anyString())).willReturn(Optional.of(mockAuth));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, "password123!");
            });
        assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("이메일과 ACTIVE 상태는 유효하나 로컬 로그인 정보(AuthLocal)가 존재하지 않으면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 계정_로그인정보_누락_예외() {
            // given
            String email = "yeojin@email.com";
            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            given(authRepository.findByEmail(anyString())).willReturn(Optional.of(mockAuth));
            given(authLocalRepository.findByAuthId(anyLong())).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, "password123!");
            });
        assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 INVALID_PASSWORD 예외가 터진다")
        public void 비밀번호_불일치_예외() {
            // given
            String email = "yeojin@email.com";
            String password = "wrongPassword";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            AuthLocal mockAuthLocal = mock(AuthLocal.class);
            given(mockAuthLocal.getPassword()).willReturn("encodedPassword");

            given(authRepository.findByEmail(anyString())).willReturn(Optional.of(mockAuth));
            given(authLocalRepository.findByAuthId(anyLong())).willReturn(Optional.of(mockAuthLocal));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, password);
            });
        assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("인증 계정 삭제 예외")
    public class DeleteAuthException {
        @Test
        @DisplayName("존재하지 않는 authId를 삭제하려고 하면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 존재하지않는계정_탈퇴시도_예외() {
            // given
            Long authId = 999L;
            given(authRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.deleteAuth(authId);
            });
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }
    }
}