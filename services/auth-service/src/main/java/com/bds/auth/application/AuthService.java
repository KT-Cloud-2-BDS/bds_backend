package com.bds.auth.application;


import com.bds.auth.infrastructure.persistence.adapter.AuthAdapter;
import com.bds.auth.infrastructure.persistence.adapter.RedisAdapter;
import com.bds.auth.presentation.dto.EmailRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthAdapter authAdapter;
    private final RedisAdapter redisAdapter;
    private final EmailService emailService;

    @Transactional
    public void sendSignUpVerificationCode(String email) {
        if (authAdapter.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        redisAdapter.put("verify:" + email, verificationCode, 3);
        emailService.sendVerificationEmail(email, verificationCode);
    }
}