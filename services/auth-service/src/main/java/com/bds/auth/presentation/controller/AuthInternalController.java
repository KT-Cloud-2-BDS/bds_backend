package com.bds.auth.presentation.controller;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.AuthCreateRequestDto;
import com.bds.auth.presentation.dto.AuthLoginRequestDto;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * member-service(Feign), gateway-service(WebClient)가 호출하는 서비스 간 내부 API.
 * /api/** 접두사를 쓰지 않아 게이트웨이의 어떤 라우팅 규칙과도 매칭되지 않으므로,
 * 외부 클라이언트는 게이트웨이를 통해 이 엔드포인트에 도달할 수 없다.
 */
@RestController
@RequestMapping("/internal/auths")
@RequiredArgsConstructor
public class AuthInternalController {

    private final AuthService authService;

    @PostMapping("/account")
    public ResponseEntity<Long> createAuthAccount(@RequestBody AuthCreateRequestDto requestDto) {
        Long authId = authService.createAccount(requestDto.email(), requestDto.password());
        return ResponseEntity.ok(authId);
    }

    @DeleteMapping("/{authId}")
    public ResponseEntity<Void> deleteAuth(@PathVariable("authId") Long authId) {
        authService.deleteAuth(authId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/blacklist")
    public ResponseEntity<Boolean> isBlacklisted(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        String accessToken = authorizationHeader.replaceFirst("^Bearer ", "");
        return ResponseEntity.ok(authService.isBlacklisted(accessToken));
    }
}
