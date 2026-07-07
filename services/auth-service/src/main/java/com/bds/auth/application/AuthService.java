package com.bds.auth.application;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.infrastructure.persistence.adapter.AuthAdapter;
import com.bds.auth.infrastructure.persistence.adapter.AuthLocalAdapter;
import com.bds.auth.presentation.dto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthAdapter authAdapter;
    private final AuthLocalAdapter authLocalAdapter;

    @Transactional
    public void signUp(SignupRequestDto requestDto) {
        if (authAdapter.existsByEmail(requestDto.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        String encodedPassword = requestDto.password();

        Auth auth = Auth.create(requestDto.email(), Role.SUPPORTER);
        Auth savedAuth = authAdapter.save(auth);

        AuthLocal authLocal = AuthLocal.create(encodedPassword, savedAuth);
        authLocalAdapter.save(authLocal);
    }
}
