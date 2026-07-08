package com.bds.auth.application;


import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.infrastructure.persistence.adapter.AuthAdapter;
import com.bds.auth.infrastructure.persistence.adapter.AuthLocalAdapter;
import com.bds.auth.infrastructure.persistence.adapter.RedisAdapter;
import com.bds.auth.infrastructure.security.JwtTokenUtil;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthAdapter authAdapter;
    private final AuthLocalAdapter authLocalAdapter;
    private final RedisAdapter redisAdapter;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    public void sendSignUpVerificationCode(String email) {
        if (authAdapter.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        redisAdapter.put("verify:" + email, verificationCode, 3);
        emailService.sendVerificationEmail(email, verificationCode);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        String redisCode = redisAdapter.get("verify:" + email);

        if (redisCode == null) {
            throw new IllegalArgumentException("인증번호가 만료되었거나 발송되지 않았습니다.");
        }
        if (!redisCode.equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        redisAdapter.delete("verify:" + email);
        redisAdapter.put("verified:" + email, "true", 10);
    }

    @Transactional
    public Long createAccount(String email, String password) {

        String ticket = redisAdapter.get("verified:" + email);
        if (!"true".equals(ticket)) {
            throw new IllegalArgumentException("인증되지 않은 이메일입니다.");
        }

        Auth newAuth = Auth.create(
            email,
            Status.ACTIVE,
            Role.SUPPORTER
        );
        Auth savedAuth = authAdapter.save(newAuth);

        String encodedPassword = passwordEncoder.encode(password);
        AuthLocal newAuthLocal = AuthLocal.create(
            savedAuth.getId(),
            encodedPassword
        );
        authLocalAdapter.save(newAuthLocal);

        return savedAuth.getId();
    }

    @Transactional
    public AuthLoginResponseDto login(String email, String password) {
        Auth auth = authAdapter.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException());

        AuthLocal authLocal = authLocalAdapter.findByAuthId(auth.getId())
            .orElseThrow(() -> new IllegalArgumentException());

        if (!passwordEncoder.matches(password, authLocal.getPassword())) {
            throw new IllegalArgumentException();
        }

        String accessToken = jwtTokenUtil.createAccessToken(auth.getId(), auth.getEmail(), auth.getRole());
        String refreshToken = jwtTokenUtil.createRefreshToken(auth.getId());

        String redisKey = "refresh:" + auth.getId();

        redisAdapter.save(redisKey, refreshToken, 7, TimeUnit.DAYS);

        return new AuthLoginResponseDto(accessToken, refreshToken);
    }
}
