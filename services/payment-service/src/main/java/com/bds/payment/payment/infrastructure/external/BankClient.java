package com.bds.payment.payment.infrastructure.external;

import com.bds.payment.payment.infrastructure.external.request.BankAccountRequestDto;
import com.bds.payment.payment.infrastructure.external.request.BankVerifyRequestDto;
import com.bds.payment.payment.infrastructure.external.response.BankAccountResponseDto;
import com.bds.payment.payment.infrastructure.external.response.BankVerifyResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankClient {

    private final RestClient restClient;

    public BankAccountResponseDto requestVerification(BankAccountRequestDto dto) {
        log.info("은행 계좌 인증코드 요청. accountNumber={}", dto.accountNumber());

        try {
            return restClient.post()
                    .uri("/api/banks/accounts")
                    .body(dto)
                    .retrieve()
                    .body(BankAccountResponseDto.class);
        } catch (HttpClientErrorException e) {
            log.error("은행 계좌 인증코드 요청 실패.");
            throw new IllegalArgumentException();
        }
    }

    public boolean confirmVerification(BankVerifyRequestDto requestDto){
        log.info("은행 계좌에 인증 확인");

        try {
            return Objects.requireNonNull(restClient.post()
                            .uri("/api/banks/accounts/verify")
                            .body(requestDto)
                            .retrieve()
                            .body(BankVerifyResponseDto.class))
                    .verified();
        } catch (HttpClientErrorException e) {
            log.error("은행 계좌 인증 실패");
            throw new IllegalArgumentException();
        }
    }
}