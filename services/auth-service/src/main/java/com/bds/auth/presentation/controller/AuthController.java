package com.bds.auth.presentation.controller;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.EmailRequestDto;
import com.bds.auth.presentation.dto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/mail")
    public ResponseEntity<Void> sendSignUpVerificationCode(@RequestBody EmailRequestDto emailRequestDto) {
        authService.sendSignUpVerificationCode(emailRequestDto.email());
        return ResponseEntity.ok().build();
    }


}
