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
import com.bds.auth.domain.entity.AuthSocial;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;

import com.bds.auth.domain.repository.AuthRepository;
import com.bds.auth.domain.repository.AuthLocalRepository;
import com.bds.auth.domain.repository.AuthSocialRepository;
import com.bds.auth.domain.repository.TokenCacheRepository;
import com.bds.auth.infrastructure.security.JwtTokenUtil;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
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
    public AuthRepository authRepository;

    @Mock
    public AuthLocalRepository authLocalRepository;

    @Mock
    public AuthSocialRepository authSocialRepository;

    @Mock
    public TokenCacheRepository tokenCacheRepository;

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
            String email = "yeojin@email.com";

            given(authRepository.findByEmail(email)).willReturn(Optional.empty());

            authService.sendSignUpVerificationCode(email);

            verify(tokenCacheRepository, times(1)).put(eq("verify:" + email), anyString(), eq(3L));
            verify(emailService, times(1)).sendVerificationEmail(eq(email), anyString());
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    public class VerifyCode {
        @Test
        @DisplayName("Redis에 저장된 코드와 일치하면 기존 코드를 지우고 완료 티켓을 발급한다")
        public void 인증코드검증_성공() {
            String email = "yeojin@email.com";
            String code = "123456";

            given(tokenCacheRepository.get("verify:" + email)).willReturn(code);

            authService.verifyCode(email, code);

            verify(tokenCacheRepository, times(1)).delete("verify:" + email);
            verify(tokenCacheRepository, times(1)).put("verified:" + email, "true", 10L);
        }
    }

    @Nested
    @DisplayName("계정 생성 기능")
    public class CreateAccount {
        @Test
        @DisplayName("이메일 인증 확인 티켓이 유효하고 중복 이메일이 없으면 신규 계정이 정상 생성된다")
        public void 계정생성_신규_성공() {
            String email = "yeojin@email.com";
            String password = "password123!";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);

            given(tokenCacheRepository.get("verified:" + email)).willReturn("true");

            given(authRepository.findByEmail(email)).willReturn(Optional.empty());
            given(authRepository.save(any(Auth.class))).willReturn(mockAuth);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            Long savedId = authService.createAccount(email, password);

            assertEquals(1L, savedId);
            verify(authRepository, times(1)).save(any(Auth.class));
            verify(authLocalRepository, times(1)).save(any(AuthLocal.class));
            verify(tokenCacheRepository, times(1)).delete("verified:" + email);
        }

        @Test
        @DisplayName("탈퇴한 기존 계정이 존재하면 상태를 ACTIVE로 복구하여 가입을 처리한다")
        public void 계정생성_기존탈퇴회원_복구_성공() {
            String email = "jinjinjala312@naver.com";
            String password = "password123!";

            Auth mockExistingAuth = mock(Auth.class);
            given(mockExistingAuth.getId()).willReturn(24L);

            given(tokenCacheRepository.get("verified:" + email)).willReturn("true");

            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockExistingAuth));
            given(authRepository.save(mockExistingAuth)).willReturn(mockExistingAuth);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            Long savedId = authService.createAccount(email, password);

            assertEquals(24L, savedId);
            verify(mockExistingAuth, times(1)).changeStatus(Status.ACTIVE);
            verify(authRepository, times(1)).save(mockExistingAuth);
            verify(authLocalRepository, times(1)).save(any(AuthLocal.class));
            verify(tokenCacheRepository, times(1)).delete("verified:" + email);
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 인증 코드 발송")
    public class SendPasswordResetVerificationCode {
        @Test
        @DisplayName("ACTIVE 계정이 존재하면 랜덤 인증 코드를 생성하여 Redis에 저장하고 메일을 발송한다")
        public void 비밀번호재설정_인증코드발송_성공() {
            String email = "yeojin@email.com";
            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));

            authService.sendPasswordResetVerificationCode(email);

            verify(tokenCacheRepository, times(1)).put(eq("pw-reset:" + email), anyString(), eq(3L));
            verify(emailService, times(1)).sendPasswordResetVerificationEmail(eq(email), anyString());
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 인증 코드 검증")
    public class VerifyPasswordResetCode {
        @Test
        @DisplayName("Redis에 저장된 코드와 일치하면 기존 코드를 지우고 변경 권한 티켓을 발급한다")
        public void 비밀번호재설정_인증코드검증_성공() {
            String email = "yeojin@email.com";
            String code = "123456";

            given(tokenCacheRepository.get("pw-reset:" + email)).willReturn(code);

            authService.verifyPasswordResetCode(email, code);

            verify(tokenCacheRepository, times(1)).delete("pw-reset:" + email);
            verify(tokenCacheRepository, times(1)).put("pw-reset-verified:" + email, "true", 3L);
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 기능")
    public class ResetPassword {
        @Test
        @DisplayName("변경 권한 티켓이 유효하면 새 비밀번호로 암호화하여 반영하고 티켓을 삭제한다")
        public void 비밀번호재설정_성공() {
            String email = "yeojin@email.com";
            String newPassword = "newPassword123!";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);

            AuthLocal authLocal = AuthLocal.of(1L, "oldEncodedPassword", 1L);

            given(tokenCacheRepository.get("pw-reset-verified:" + email)).willReturn("true");
            given(authRepository.findByEmail(email)).willReturn(Optional.of(mockAuth));
            given(authLocalRepository.findByAuthId(1L)).willReturn(Optional.of(authLocal));
            given(passwordEncoder.encode(newPassword)).willReturn("newEncodedPassword");

            authService.resetPassword(email, newPassword);

            assertEquals("newEncodedPassword", authLocal.getPassword());
            verify(authLocalRepository, times(1)).save(authLocal);
            verify(tokenCacheRepository, times(1)).delete("pw-reset-verified:" + email);
        }
    }

    @Nested
    @DisplayName("로그인 기능")
    public class Login {
        @Test
        @DisplayName("이메일과 비밀번호가 일치하고 ACTIVE 계정이면 토큰 세트가 정상 발급된다")
        public void 로그인_성공() {
            String email = "yeojin@email.com";
            String password = "password123!";

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);
            given(mockAuth.getEmail()).willReturn(email);
            given(mockAuth.getRole()).willReturn(Role.SUPPORTER);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            AuthLocal mockAuthLocal = mock(AuthLocal.class);
            given(mockAuthLocal.getPassword()).willReturn("encodedPassword");

            given(authRepository.findByEmail(anyString())).willReturn(Optional.of(mockAuth));
            given(authLocalRepository.findByAuthId(anyLong())).willReturn(Optional.of(mockAuthLocal));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(jwtTokenUtil.createAccessToken(anyLong(), anyString(), any(Role.class))).willReturn("accessToken");
            given(jwtTokenUtil.createRefreshToken(anyLong())).willReturn("refreshToken");

            AuthLoginResponseDto response = authService.login(email, password);

            assertNotNull(response);
            assertEquals("accessToken", response.accessToken());
            assertEquals("refreshToken", response.refreshToken());
            verify(tokenCacheRepository, times(1)).save(eq("refresh:1"), eq("refreshToken"), eq(7L), eq(TimeUnit.DAYS));
        }
    }

    @Nested
    @DisplayName("소셜 로그인 기능")
    public class SocialLogin {
        @Test
        @DisplayName("연동된 소셜 계정이 없으면 새 Auth+AuthSocial 계정을 생성하고 토큰을 발급한다")
        public void 소셜로그인_신규계정_성공() {
            // given
            String provider = "naver";
            String providerId = "naver-12345";
            String email = "social@email.com";

            given(authSocialRepository.findByProviderAndProviderId(provider, providerId)).willReturn(Optional.empty());

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(1L);
            given(mockAuth.getEmail()).willReturn(email);
            given(mockAuth.getRole()).willReturn(Role.SUPPORTER);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            given(authRepository.save(any(Auth.class))).willReturn(mockAuth);
            given(jwtTokenUtil.createAccessToken(1L, email, Role.SUPPORTER)).willReturn("accessToken");
            given(jwtTokenUtil.createRefreshToken(1L)).willReturn("refreshToken");

            // when
            AuthLoginResponseDto response = authService.processSocialLogin(provider, providerId, email);

            // then
            assertEquals("accessToken", response.accessToken());
            assertEquals("refreshToken", response.refreshToken());
            verify(authRepository, times(1)).save(any(Auth.class));
            verify(authSocialRepository, times(1)).save(any(AuthSocial.class));
            verify(tokenCacheRepository, times(1)).save(eq("refresh:1"), eq("refreshToken"), eq(7L), eq(TimeUnit.DAYS));
        }

        @Test
        @DisplayName("이미 연동된 소셜 계정이면 기존 Auth로 로그인 처리하고 토큰을 발급한다")
        public void 소셜로그인_기존계정_성공() {
            // given
            String provider = "naver";
            String providerId = "naver-12345";
            String email = "social@email.com";
            Long authId = 5L;

            AuthSocial existingSocial = AuthSocial.of(1L, providerId, provider, email, authId);
            given(authSocialRepository.findByProviderAndProviderId(provider, providerId)).willReturn(Optional.of(existingSocial));

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(authId);
            given(mockAuth.getEmail()).willReturn(email);
            given(mockAuth.getRole()).willReturn(Role.SUPPORTER);
            given(mockAuth.getStatus()).willReturn(Status.ACTIVE);

            given(authRepository.findById(authId)).willReturn(Optional.of(mockAuth));
            given(jwtTokenUtil.createAccessToken(authId, email, Role.SUPPORTER)).willReturn("accessToken");
            given(jwtTokenUtil.createRefreshToken(authId)).willReturn("refreshToken");

            // when
            AuthLoginResponseDto response = authService.processSocialLogin(provider, providerId, email);

            // then
            assertEquals("accessToken", response.accessToken());
            verify(authRepository, times(0)).save(any(Auth.class));
            verify(authSocialRepository, times(0)).save(any(AuthSocial.class));
        }

        @Test
        @DisplayName("연동된 계정이 탈퇴(DELETED) 상태였다면 ACTIVE로 복구하고 토큰을 발급한다")
        public void 소셜로그인_탈퇴계정_복구_성공() {
            // given
            String provider = "naver";
            String providerId = "naver-12345";
            String email = "social@email.com";
            Long authId = 5L;

            AuthSocial existingSocial = AuthSocial.of(1L, providerId, provider, email, authId);
            given(authSocialRepository.findByProviderAndProviderId(provider, providerId)).willReturn(Optional.of(existingSocial));

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(authId);
            given(mockAuth.getEmail()).willReturn(email);
            given(mockAuth.getRole()).willReturn(Role.SUPPORTER);
            given(mockAuth.getStatus()).willReturn(Status.DELETED);

            given(authRepository.findById(authId)).willReturn(Optional.of(mockAuth));
            given(authRepository.save(mockAuth)).willReturn(mockAuth);
            given(jwtTokenUtil.createAccessToken(authId, email, Role.SUPPORTER)).willReturn("accessToken");
            given(jwtTokenUtil.createRefreshToken(authId)).willReturn("refreshToken");

            // when
            authService.processSocialLogin(provider, providerId, email);

            // then
            verify(mockAuth, times(1)).changeStatus(Status.ACTIVE);
            verify(authRepository, times(1)).save(mockAuth);
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 재발급 기능")
    public class ReissueToken {
        @Test
        @DisplayName("유효한 refresh token이 Redis에 저장된 값과 일치하면 기존 토큰을 삭제하고 새 토큰 세트를 발급한다")
        public void 토큰재발급_성공() {
            // given
            Long authId = 1L;
            String oldRefreshToken = "oldRefreshToken";
            String redisKey = "refresh:" + authId;

            Claims claims = Jwts.claims().subject(String.valueOf(authId)).build();
            given(jwtTokenUtil.parseClaims(oldRefreshToken)).willReturn(claims);
            given(tokenCacheRepository.get(redisKey)).willReturn(oldRefreshToken);

            Auth mockAuth = mock(Auth.class);
            given(mockAuth.getId()).willReturn(authId);
            given(mockAuth.getEmail()).willReturn("yeojin@email.com");
            given(mockAuth.getRole()).willReturn(Role.SUPPORTER);
            given(authRepository.findById(authId)).willReturn(Optional.of(mockAuth));

            given(jwtTokenUtil.createAccessToken(authId, "yeojin@email.com", Role.SUPPORTER)).willReturn("newAccessToken");
            given(jwtTokenUtil.createRefreshToken(authId)).willReturn("newRefreshToken");

            // when
            AuthLoginResponseDto response = authService.reissueToken(oldRefreshToken);

            // then
            assertEquals("newAccessToken", response.accessToken());
            assertEquals("newRefreshToken", response.refreshToken());
            verify(tokenCacheRepository, times(1)).delete(redisKey);
            verify(tokenCacheRepository, times(1)).save(eq(redisKey), eq("newRefreshToken"), eq(7L), eq(TimeUnit.DAYS));
        }
    }

    @Nested
    @DisplayName("로그아웃 기능")
    public class Logout {
        @Test
        @DisplayName("로그아웃하면 refresh token을 삭제하고 access token 잔여 만료시간만큼 블랙리스트에 등록한다")
        public void 로그아웃_성공() {
            // given
            Long authId = 1L;
            String accessToken = "accessToken";
            long remainingMillis = 60_000L;
            Claims claims = Jwts.claims()
                .subject(String.valueOf(authId))
                .expiration(new Date(System.currentTimeMillis() + remainingMillis))
                .build();

            given(jwtTokenUtil.parseClaims(accessToken)).willReturn(claims);

            // when
            authService.logout(authId, accessToken);

            // then
            verify(tokenCacheRepository, times(1)).delete("refresh:" + authId);
            verify(tokenCacheRepository, times(1))
                .save(eq("blacklist:" + accessToken), eq("true"), anyLong(), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("access token이 이미 만료된 상태라면 블랙리스트에 등록하지 않는다")
        public void 로그아웃_이미만료된토큰_블랙리스트미등록() {
            // given
            Long authId = 1L;
            String accessToken = "expiredAccessToken";
            Claims claims = Jwts.claims()
                .subject(String.valueOf(authId))
                .expiration(new Date(System.currentTimeMillis() - 1_000L))
                .build();

            given(jwtTokenUtil.parseClaims(accessToken)).willReturn(claims);

            // when
            authService.logout(authId, accessToken);

            // then
            verify(tokenCacheRepository, times(1)).delete("refresh:" + authId);
            verify(tokenCacheRepository, times(0))
                .save(anyString(), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
        }
    }

    @Nested
    @DisplayName("블랙리스트 조회 기능")
    public class IsBlacklisted {
        @Test
        @DisplayName("Redis에 블랙리스트 키가 존재하면 true를 반환한다")
        public void 블랙리스트조회_등록된토큰_true() {
            // given
            String accessToken = "blacklistedAccessToken";
            given(tokenCacheRepository.get("blacklist:" + accessToken)).willReturn("true");

            // when
            boolean result = authService.isBlacklisted(accessToken);

            // then
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Redis에 블랙리스트 키가 없으면 false를 반환한다")
        public void 블랙리스트조회_미등록토큰_false() {
            // given
            String accessToken = "normalAccessToken";
            given(tokenCacheRepository.get("blacklist:" + accessToken)).willReturn(null);

            // when
            boolean result = authService.isBlacklisted(accessToken);

            // then
            assertEquals(false, result);
        }
    }

    @Nested
    @DisplayName("로그인 상태 비밀번호 변경 기능")
    public class ChangePassword {
        @Test
        @DisplayName("현재 비밀번호가 일치하면 새 비밀번호로 암호화하여 반영한다")
        public void 비밀번호변경_성공() {
            // given
            Long authId = 1L;
            String currentPassword = "oldPassword123!";
            String newPassword = "newPassword123!";

            AuthLocal authLocal = AuthLocal.of(1L, "oldEncodedPassword", authId);

            given(authLocalRepository.findByAuthId(authId)).willReturn(Optional.of(authLocal));
            given(passwordEncoder.matches(currentPassword, "oldEncodedPassword")).willReturn(true);
            given(passwordEncoder.encode(newPassword)).willReturn("newEncodedPassword");

            // when
            authService.changePassword(authId, currentPassword, newPassword);

            // then
            assertEquals("newEncodedPassword", authLocal.getPassword());
            verify(authLocalRepository, times(1)).save(authLocal);
        }
    }

    @Nested
    @DisplayName("유저 권한(Role) 전환 기능")
    public class SwitchRole {

        @Test
        @DisplayName("존재하는 계정인 경우 권한을 전환하고 변경된 새로운 권한을 반환한다")
        public void 권한전환_성공() {
            // given
            Long authId = 1L;
            Auth mockAuth = mock(Auth.class);

            // 처음에는 SUPPORTER였다가 switchRole() 호출 후 MAKER로 변하는 상황 모킹
            given(authRepository.findById(authId)).willReturn(Optional.of(mockAuth));
            given(mockAuth.getRole()).willReturn(Role.MAKER);
            given(authRepository.save(mockAuth)).willReturn(mockAuth);

            // when
            Role updatedRole = authService.switchRole(authId);

            // then
            assertEquals(Role.MAKER, updatedRole);
            verify(mockAuth, times(1)).switchRole(); // 엔티티 내부 권한 변경 메서드가 실행되었는지 검증
            verify(authRepository, times(1)).findById(authId);
            verify(authRepository, times(1)).save(mockAuth);
        }

        @Test
        @DisplayName("존재하지 않는 계정 ID인 경우 ACCOUNT_NOT_FOUND 예외를 발생시킨다")
        public void 권한전환_실패_존재하지않는계정() {
            // given
            Long nonExistentAuthId = 999L;
            given(authRepository.findById(nonExistentAuthId)).willReturn(Optional.empty());

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                com.bds.auth.global.exception.BusinessException.class,
                () -> authService.switchRole(nonExistentAuthId)
            );

            // 엔티티 조작이나 저장이 일어나지 않아야 함을 검증
            verify(authRepository, times(1)).findById(nonExistentAuthId);
            verify(authRepository, times(0)).save(any(Auth.class));
        }
    }

    @Nested
    @DisplayName("계정 삭제 기능")
    public class DeleteAuth {
        @Test
        @DisplayName("계정이 존재하면 상태를 DELETED로 변경하고 로컬 로그인 정보 및 리프레시 토큰을 삭제한다")
        public void 계정삭제_성공() {
            Long authId = 1L;
            Auth mockAuth = mock(Auth.class);

            given(authRepository.findById(anyLong())).willReturn(Optional.of(mockAuth));

            authService.deleteAuth(authId);

            verify(authRepository, times(1)).softDelete(authId);
            verify(authLocalRepository, times(1)).deleteByAuthId(authId);
            verify(tokenCacheRepository, times(1)).delete("refresh:" + authId);
        }
    }
}