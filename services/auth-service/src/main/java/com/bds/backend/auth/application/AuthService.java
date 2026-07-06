package com.bds.backend.auth.application;

import com.bds.backend.auth.domain.entity.Auth;
import com.bds.backend.auth.domain.entity.AuthLocal;
import com.bds.backend.auth.domain.entity.enums.Role;
import com.bds.backend.auth.domain.entity.enums.Status;
import com.bds.backend.auth.domain.repository.AuthLocalRepository;
import com.bds.backend.auth.domain.repository.AuthRepository;
import com.bds.backend.auth.presentation.dto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthRepository authRepo;
    private final AuthLocalRepository authLocalRepo;


    @Transactional
    public void signUp(SignupRequestDto requestDto) {
        if(authRepo.existByEmail(requestDto.email())){
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        String encodedPassword = requestDto.password();

        Auth auth = Auth.builder()
            .email(requestDto.email())
            .status(Status.ACTIVE)
            .role(Role.SUPPORTER)
            .build();

        authRepo.save(auth);

        AuthLocal authLocal = AuthLocal.builder()
            .password(encodedPassword)
            .auth(auth)
            .build();

        authLocalRepo.save(authLocal);

    }
}
