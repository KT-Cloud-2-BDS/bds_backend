package com.bds.payment.bank.infrastructure.persistence.bankVerifyCode;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@Table(name = "bank_verify_code")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankVerifyCodeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bankCode;

    //TODO: 중복 계좌 재요청 시 코드 갱신에 대한 케이스 생각 해볼 것
    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private String verifyCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
