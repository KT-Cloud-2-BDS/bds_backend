package com.bds.member.infrastructure.persistence.feignClient;

import com.bds.member.presentation.dto.AuthCreateRequestDto;
import com.bds.member.presentation.dto.AuthLoginRequestDto;
import com.bds.member.presentation.dto.AuthLoginResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "auth-client", url = "http://localhost:8081")
public interface AuthFeignClient {

    @PostMapping("/api/auth/account")
    ResponseEntity<Long> createAuthAccount(@RequestBody AuthCreateRequestDto requestDto);

    @PostMapping("/api/auth/login")
    ResponseEntity<AuthLoginResponseDto> login(@RequestBody AuthLoginRequestDto requestDto);
}