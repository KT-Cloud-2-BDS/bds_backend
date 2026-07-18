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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
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

            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));

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

            given(tokenCacheRepository.get("verify:" + email)).willReturn(null);

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
    @DisplayName("비밀번호 재설정 인증 코드 발송 예외")
    public class SendPasswordResetVerificationCodeException {
        @Test
        @DisplayName("가입되지 않은 이메일이면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 가입안된이메일_예외() {
            // given
            String email = "notfound@email.com";
            given(authRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.sendPasswordResetVerificationCode(email);
            });
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("ACTIVE 상태가 아닌 계정이면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 비활성계정_예외() {
            // given
            String email = "deleted@email.com";
            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getStatus()).willReturn(Status.DELETED);
            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.sendPasswordResetVerificationCode(email);
            });
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 인증 코드 검증 예외")
    public class VerifyPasswordResetCodeException {
        @Test
        @DisplayName("Redis에 인증 코드가 만료되어 존재하지 않으면 VERIFICATION_CODE_EXPIRED 예외가 터진다")
        public void 인증코드_만료_예외() {
            // given
            String email = "yeojin@email.com";
            given(tokenCacheRepository.get("pw-reset:" + email)).willReturn(null);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.verifyPasswordResetCode(email, "123456");
            });
            assertEquals(ErrorCode.VERIFICATION_CODE_EXPIRED, exception.getErrorCode());
        }

        @Test
        @DisplayName("Redis에 저장된 코드와 입력받은 코드가 일치하지 않으면 VERIFICATION_CODE_MISMATCH 예외가 터진다")
        public void 인증코드_불일치_예외() {
            // given
            String email = "yeojin@email.com";
            given(tokenCacheRepository.get("pw-reset:" + email)).willReturn("123456");

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.verifyPasswordResetCode(email, "999999");
            });
            assertEquals(ErrorCode.VERIFICATION_CODE_MISMATCH, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 예외")
    public class ResetPasswordException {
        @Test
        @DisplayName("변경 권한 티켓이 없으면 UNVERIFIED_EMAIL 예외가 터진다")
        public void 변경권한없음_예외() {
            // given
            String email = "yeojin@email.com";
            given(tokenCacheRepository.get("pw-reset-verified:" + email)).willReturn(null);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.resetPassword(email, "newPassword123!");
            });
            assertEquals(ErrorCode.UNVERIFIED_EMAIL, exception.getErrorCode());
        }

        @Test
        @DisplayName("변경 권한 티켓은 유효하나 계정이 존재하지 않으면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 계정없음_예외() {
            // given
            String email = "yeojin@email.com";
            given(tokenCacheRepository.get("pw-reset-verified:" + email)).willReturn("true");
            given(authRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.resetPassword(email, "newPassword123!");
            });
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("로그인 예외")
    public class LoginException {
        @Test
        @DisplayName("존재하지 않는 이메일 주소라면 INVALID_LOGIN_CREDENTIALS 예외가 터진다")
        public void 계정_존재하지않음_이메일오류_예외() {
            // given
            String email = "wrong@email.com";

            given(authRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, "password123!");
            });
            assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("계정은 존재하나 ACTIVE 상태가 아니라면 INVALID_LOGIN_CREDENTIALS 예외가 터진다")
        public void 비활성화_계정_로그인_차단_예외() {
            // given
            String email = "yeojin@email.com";
            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getStatus()).willReturn(Status.DELETED);

            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, "password123!");
            });
            assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("이메일과 ACTIVE 상태는 유효하나 로컬 로그인 정보(AuthLocal)가 존재하지 않으면 INVALID_LOGIN_CREDENTIALS 예외가 터진다")
        public void 계정_로그인정보_누락_예외() {
            // given
            String email = "yeojin@email.com";
            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));
            given(authLocalRepository.findByAuthId(1L)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, "password123!");
            });
            assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 INVALID_LOGIN_CREDENTIALS 예외가 터진다")
        public void 비밀번호_불일치_예외() {
            // given
            String email = "yeojin@email.com";
            String password = "wrongPassword";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            AuthLocal mockAuthLocal = mock(AuthLocal.class);
            given(mockAuthLocal.getPassword()).willReturn("encodedPassword");

            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));
            given(authLocalRepository.findByAuthId(1L)).willReturn(Optional.of(mockAuthLocal));
            given(passwordEncoder.matches(password, "encodedPassword")).willReturn(false);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.login(email, password);
            });
            assertEquals(ErrorCode.INVALID_LOGIN_CREDENTIALS, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 재발급 예외")
    public class ReissueTokenException {

        @Test
        @DisplayName("서명이 유효하지 않거나 형식이 손상된 refresh token이면 INVALID_REFRESH_TOKEN 예외가 터진다")
        public void 토큰_파싱실패_예외() {
            // given
            String malformedToken = "malformed.token.value";
            given(jwtTokenUtil.parseClaims(malformedToken)).willThrow(new MalformedJwtException("잘못된 토큰"));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.reissueToken(malformedToken);
            });
            assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("Redis에 저장된 refresh token이 없으면(만료/로그아웃) INVALID_REFRESH_TOKEN 예외가 터진다")
        public void 토큰_레디스없음_예외() {
            // given
            Long authId = 1L;
            String refreshToken = "refreshToken";
            Claims claims = Jwts.claims().subject(String.valueOf(authId)).build();

            given(jwtTokenUtil.parseClaims(refreshToken)).willReturn(claims);
            given(tokenCacheRepository.get("refresh:" + authId)).willReturn(null);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.reissueToken(refreshToken);
            });
            assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("Redis에 저장된 값과 요청받은 refresh token이 일치하지 않으면 INVALID_REFRESH_TOKEN 예외가 터진다")
        public void 토큰_불일치_예외() {
            // given
            Long authId = 1L;
            String requestedToken = "requestedToken";
            Claims claims = Jwts.claims().subject(String.valueOf(authId)).build();

            given(jwtTokenUtil.parseClaims(requestedToken)).willReturn(claims);
            given(tokenCacheRepository.get("refresh:" + authId)).willReturn("differentStoredToken");

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.reissueToken(requestedToken);
            });
            assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("토큰은 유효하나 계정이 존재하지 않으면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 토큰_계정없음_예외() {
            // given
            Long authId = 999L;
            String refreshToken = "refreshToken";
            Claims claims = Jwts.claims().subject(String.valueOf(authId)).build();

            given(jwtTokenUtil.parseClaims(refreshToken)).willReturn(claims);
            given(tokenCacheRepository.get("refresh:" + authId)).willReturn(refreshToken);
            given(authRepository.findById(authId)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.reissueToken(refreshToken);
            });
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("로그인 상태 비밀번호 변경 예외")
    public class ChangePasswordException {
        @Test
        @DisplayName("계정의 로컬 로그인 정보가 존재하지 않으면 ACCOUNT_NOT_FOUND 예외가 터진다")
        public void 로컬로그인정보없음_예외() {
            // given
            Long authId = 999L;
            given(authLocalRepository.findByAuthId(authId)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.changePassword(authId, "currentPassword", "newPassword123!");
            });
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 INVALID_PASSWORD 예외가 터진다")
        public void 현재비밀번호_불일치_예외() {
            // given
            Long authId = 1L;
            AuthLocal authLocal = AuthLocal.of(1L, "encodedPassword", authId);

            given(authLocalRepository.findByAuthId(authId)).willReturn(Optional.of(authLocal));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.changePassword(authId, "wrongPassword", "newPassword123!");
            });
            assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());
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

            given(authRepository.findById(authId)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.deleteAuth(authId);
            });
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }
    }
}