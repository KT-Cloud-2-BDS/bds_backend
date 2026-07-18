package com.bds.auth.application;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.domain.repository.AuthRepository;
import com.bds.auth.domain.repository.AuthLocalRepository;
import com.bds.auth.domain.repository.TokenCacheRepository;
import com.bds.auth.global.exception.BusinessException;
import com.bds.auth.global.exception.ErrorCode;
import com.bds.auth.infrastructure.security.JwtTokenUtil;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {


    private final AuthRepository authRepository;
    private final AuthLocalRepository authLocalRepository;
    private final TokenCacheRepository tokenCacheRepository;

    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public void sendSignUpVerificationCode(String email) {

        Optional<Auth> existingAuth = authRepository.findByEmail(email);

        if (existingAuth.isPresent() && existingAuth.get().getStatus() == Status.ACTIVE) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String verificationCode = String.valueOf(SECURE_RANDOM.nextInt(900_000) + 100_000);

        tokenCacheRepository.put("verify:" + email, verificationCode, 3);
        emailService.sendVerificationEmail(email, verificationCode);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        String redisCode = tokenCacheRepository.get("verify:" + email);

        if (redisCode == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!code.equals(redisCode)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        tokenCacheRepository.delete("verify:" + email);
        tokenCacheRepository.put("verified:" + email, "true", 10);
    }

    @Transactional
    public Long createAccount(String email, String password) {
        String ticket = tokenCacheRepository.get("verified:" + email);
        if (!"true".equals(ticket)) {
            throw new BusinessException(ErrorCode.UNVERIFIED_EMAIL);
        }

        Optional<Auth> existingAuth = authRepository.findByEmail(email);

        Auth savedAuth;

        if (existingAuth.isPresent()) {
            Auth auth = existingAuth.get();

            if (auth.getStatus() == Status.ACTIVE) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }

            auth.changeStatus(Status.ACTIVE);
            savedAuth = authRepository.save(auth);

        } else {
            Auth newAuth = Auth.create(email, Status.ACTIVE, Role.SUPPORTER);
            savedAuth = authRepository.save(newAuth);
        }

        String encodedPassword = passwordEncoder.encode(password);

        AuthLocal newAuthLocal = AuthLocal.create(savedAuth.getId(), encodedPassword);
        authLocalRepository.save(newAuthLocal);
        tokenCacheRepository.delete("verified:" + email);

        return savedAuth.getId();
    }

    @Transactional
    public AuthLoginResponseDto login(String email, String password) {
        Auth auth = authRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (auth.getStatus() != Status.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        AuthLocal authLocal = authLocalRepository.findByAuthId(auth.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (!passwordEncoder.matches(password, authLocal.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        String accessToken = jwtTokenUtil.createAccessToken(auth.getId(), auth.getEmail(), auth.getRole());
        String refreshToken = jwtTokenUtil.createRefreshToken(auth.getId());

        String redisKey = "refresh:" + auth.getId();
        tokenCacheRepository.save(redisKey, refreshToken, 7, TimeUnit.DAYS);

        return new AuthLoginResponseDto(accessToken, refreshToken);
    }

    @Transactional
    public AuthLoginResponseDto reissueToken(String refreshToken) {
        Long authId;
        try {
            Claims claims = jwtTokenUtil.parseClaims(refreshToken);
            authId = Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String redisKey = "refresh:" + authId;
        String storedRefreshToken = tokenCacheRepository.get(redisKey);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Auth auth = authRepository.findById(authId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        tokenCacheRepository.delete(redisKey);

        String newAccessToken = jwtTokenUtil.createAccessToken(auth.getId(), auth.getEmail(), auth.getRole());
        String newRefreshToken = jwtTokenUtil.createRefreshToken(auth.getId());

        tokenCacheRepository.save("refresh:" + auth.getId(), newRefreshToken, 7, TimeUnit.DAYS);

        return new AuthLoginResponseDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long authId, String accessToken) {
        tokenCacheRepository.delete("refresh:" + authId);

        Claims claims = jwtTokenUtil.parseClaims(accessToken);
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();

        if (remainingMillis > 0) {
            tokenCacheRepository.save("blacklist:" + accessToken, "true", remainingMillis, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return tokenCacheRepository.get("blacklist:" + accessToken) != null;
    }

    @Transactional
    public Role switchRole(Long authId) {
        Auth auth = authRepository.findById(authId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        auth.switchRole();
        Auth savedAuth = authRepository.save(auth);

        return savedAuth.getRole();
    }

    @Transactional
    public void deleteAuth(Long authId) {
        authRepository.findById(authId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        authRepository.softDelete(authId);

        authLocalRepository.deleteByAuthId(authId);
        tokenCacheRepository.delete("refresh:" + authId);
    }
}