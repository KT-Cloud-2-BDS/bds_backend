package com.bds.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bds.auth.application.AuthService;
import com.bds.auth.application.EmailService;
import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.infrastructure.persistence.adapter.AuthAdapter;
import com.bds.auth.infrastructure.persistence.adapter.AuthLocalAdapter;
import com.bds.auth.infrastructure.persistence.adapter.RedisAdapter;
import com.bds.auth.infrastructure.security.JwtTokenUtil;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트 - 성공 케이스")
public class AuthServiceUnitTest {

    @InjectMocks
    public AuthService authService;

    @Mock
    public AuthAdapter authAdapter;

    @Mock
    public AuthLocalAdapter authLocalAdapter;

    @Mock
    public RedisAdapter redisAdapter;

    @Mock
    public EmailService emailService;

    @Mock
    public PasswordEncoder passwordEncoder;

    @Mock
    public JwtTokenUtil jwtTokenUtil;

    @Nested
    @DisplayName("회원가입 인증 코드 발송")
    public class SendSignUpVerificationCode {
        @Test
        @DisplayName("중복 이메일이 없으면 랜덤 인증 코드를 생성하여 Redis에 저장하고 메일을 발송한다")
        public void 인증코드발송_성공() {
            // given
            String email = "yeojin@email.com";
            given(authAdapter.existsByEmailAndStatus(anyString(), eq(Status.ACTIVE))).willReturn(false);

            // when
            authService.sendSignUpVerificationCode(email);

            // then
            verify(redisAdapter, times(1)).put(eq("verify:" + email), anyString(), eq(3L));
            verify(emailService, times(1)).sendVerificationEmail(eq(email), anyString());
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    public class VerifyCode {
        @Test
        @DisplayName("Redis에 저장된 코드와 일치하면 기존 코드를 지우고 완료 티켓을 발급한다")
        public void 인증코드검증_성공() {
            // given
            String email = "yeojin@email.com";
            String code = "123456";
            given(redisAdapter.get("verify:" + email)).willReturn(code);

            // when
            authService.verifyCode(email, code);

            // then
            verify(redisAdapter, times(1)).delete("verify:" + email);
            verify(redisAdapter, times(1)).put("verified:" + email, "true", 10L);
        }
    }

    @Nested
    @DisplayName("계정 생성 기능")
    public class CreateAccount {
        @Test
        @DisplayName("이메일 인증 확인 티켓이 유효하고 중복 이메일이 없으면 신규 계정이 정상 생성된다")
        public void 계정생성_신규_성공() {
            // given
            String email = "yeojin@email.com";
            String password = "password123!";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);

            given(redisAdapter.get("verified:" + email)).willReturn("true");
            given(authAdapter.existsByEmailAndStatus(email, Status.ACTIVE)).willReturn(false);

            given(authAdapter.findByEmail(email)).willReturn(Optional.empty());
            given(authAdapter.save(any(Auth.class))).willReturn(mockAuth);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            // when
            Long savedId = authService.createAccount(email, password);

            // then
            assertEquals(1L, savedId);
            verify(authAdapter, times(1)).save(any(Auth.class));
            verify(authLocalAdapter, times(1)).save(any(AuthLocal.class));
            verify(redisAdapter, times(1)).delete("verified:" + email);
        }

        @Test
        @DisplayName("탈퇴한 기존 계정이 존재하면 상태를 ACTIVE로 복구하여 가입을 처리한다")
        public void 계정생성_기존탈퇴회원_복구_성공() {
            // given
            String email = "jinjinjala312@naver.com";
            String password = "password123!";

            Auth mockExistingAuth = mock(Auth.class);
            given(mockExistingAuth.getId()).willReturn(24L);

            given(redisAdapter.get("verified:" + email)).willReturn("true");
            given(authAdapter.existsByEmailAndStatus(email, Status.ACTIVE)).willReturn(false);

            given(authAdapter.findByEmail(email)).willReturn(Optional.of(mockExistingAuth));
            given(authAdapter.save(mockExistingAuth)).willReturn(mockExistingAuth);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            // when
            Long savedId = authService.createAccount(email, password);

            // then
            assertEquals(24L, savedId);
            verify(mockExistingAuth, times(1)).changeStatus(Status.ACTIVE);
            verify(authAdapter, times(1)).save(mockExistingAuth);
            verify(authLocalAdapter, times(1)).save(any(AuthLocal.class));
            verify(redisAdapter, times(1)).delete("verified:" + email);
        }
    }

    @Nested
    @DisplayName("로그인 기능")
    public class Login {
        @Test
        @DisplayName("이메일과 비밀번호가 일치하고 ACTIVE 계정이면 토큰 세트가 정상 발급된다")
        public void 로그인_성공() {
            // given
            String email = "yeojin@email.com";
            String password = "password123!";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);
            given(mockAuth.getEmail()).willReturn(email);
            given(mockAuth.getRole()).willReturn(Role.SUPPORTER);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            AuthLocal mockAuthLocal = mock(AuthLocal.class);
            given(mockAuthLocal.getPassword()).willReturn("encodedPassword");

            given(authAdapter.findByEmail(anyString())).willReturn(Optional.of(mockAuth));
            given(authLocalAdapter.findByAuthId(anyLong())).willReturn(Optional.of(mockAuthLocal));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(jwtTokenUtil.createAccessToken(anyLong(), anyString(), any(Role.class))).willReturn("accessToken");
            given(jwtTokenUtil.createRefreshToken(anyLong())).willReturn("refreshToken");

            // when
            AuthLoginResponseDto response = authService.login(email, password);

            // then
            assertNotNull(response);
            assertEquals("accessToken", response.accessToken());
            assertEquals("refreshToken", response.refreshToken());
            verify(redisAdapter, times(1)).save(eq("refresh:1"), eq("refreshToken"), eq(7L), eq(TimeUnit.DAYS));
        }
    }

    @Nested
    @DisplayName("계정 삭제 기능")
    public class DeleteAuth {
        @Test
        @DisplayName("계정이 존재하면 상태를 DELETED로 변경하고 로컬 로그인 정보 및 리프레시 토큰을 삭제한다")
        public void 계정삭제_성공() {
            // given
            Long authId = 1L;
            Auth mockAuth = mock(Auth.class);
            given(authAdapter.findById(anyLong())).willReturn(Optional.of(mockAuth));

            // when
            authService.deleteAuth(authId);

            // then
            verify(mockAuth, times(1)).changeStatus(Status.DELETED);
            verify(authAdapter, times(1)).save(mockAuth);
            verify(authLocalAdapter, times(1)).deleteByAuthId(authId);
            verify(redisAdapter, times(1)).delete("refresh:" + authId);
        }
    }
}