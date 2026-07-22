package com.bds.member.infrastructure.persistence.feignClient;

import com.bds.member.presentation.dto.AuthCreateRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "auth-service", url = "${client.auth-service.url:}")
public interface AuthFeignClient {

    @PostMapping("/internal/auths/account")
    ResponseEntity<Long> createAuthAccount(@RequestBody AuthCreateRequestDto requestDto);

    @DeleteMapping("/internal/auths/{authId}")
    ResponseEntity<Void> deleteAuth(@PathVariable("authId") Long authId);
}