-- =========================================
-- 1. 개인 월렛
-- =========================================
CREATE TABLE wallet
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id  BIGINT   NOT NULL UNIQUE,
    balance    BIGINT   NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_wallet_balance CHECK (balance >= 0)
);

-- =========================================
-- 2. 등록 계좌 (1원 인증 포함)
-- =========================================
CREATE TABLE bank_account
(
    wallet_id      BIGINT PRIMARY KEY,
    bank_code      VARCHAR(10) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    holder_name    VARCHAR(50) NOT NULL,
    is_verified    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_bank_account_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);


-- =========================================
-- 3. 펀딩 결제 이력 (후원자별 결제건)
--    product_account는 주문 도메인으로 이관되어
--    product_id, order_id는 외부 참조값 (FK 없음)
-- =========================================
CREATE TABLE funding_payment
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id     BIGINT      NOT NULL UNIQUE,
    wallet_id    BIGINT      NOT NULL,
    product_id   BIGINT      NOT NULL,
    tran_seq_no  CHAR(36)    NOT NULL,
    amount       INT         NOT NULL,
    payment_type VARCHAR(20) NOT NULL,                   -- INSTANT | RESERVED
    status       VARCHAR(20) NOT NULL DEFAULT 'SUCCESS', -- SUCCESS | REFUNDED | FAILED | RESERVED
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_funding_payment_tran_seq_no UNIQUE (tran_seq_no),
    CONSTRAINT chk_funding_payment_amount CHECK (amount > 0),
    CONSTRAINT fk_funding_payment_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),

    INDEX        idx_funding_payment_product (product_id),
    INDEX        idx_funding_payment_wallet (wallet_id)
);


-- =========================================
-- 4. 개인 월렛 거래 이력 (충전 / 출금 / 펀딩결제 / 펀딩환불 / 정산수령)
--    type: 잔액 증감 방향, reason: 발생 사유(카테고리)
--    funding_payment_id: 펀딩 관련 거래일 때만 채움 (불변 행이라 join 안전)
-- =========================================
CREATE TABLE payment_history
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id          BIGINT      NOT NULL,
    funding_payment_id BIGINT NULL,
    tran_seq_no        CHAR(36)    NOT NULL,
    type               VARCHAR(20) NOT NULL,                   -- DEPOSIT | WITHDRAWAL
    reason             VARCHAR(20) NOT NULL,                   -- CHARGE | WITHDRAW | FUNDING_PAYMENT | FUNDING_REFUND | SETTLEMENT
    message            VARCHAR(50),
    amount             BIGINT      NOT NULL,
    balance_after      BIGINT      NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'SUCCESS', -- SUCCESS | FAILED | COMPENSATED
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_payment_history_tran_seq_no UNIQUE (tran_seq_no),
    CONSTRAINT chk_payment_history_amount CHECK (amount > 0),
    CONSTRAINT fk_payment_history_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_payment_history_funding FOREIGN KEY (funding_payment_id) REFERENCES funding_payment (id),

    INDEX              idx_payment_history_wallet (wallet_id),
    INDEX              idx_payment_history_funding (funding_payment_id)
);

-- =========================================
-- 5. [가상 금융망] 1원 인증 기록
-- =========================================
CREATE TABLE bank_verify_code
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    bank_code      VARCHAR(10) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    holder_name    VARCHAR(50) NOT NULL,
    verify_code    VARCHAR(10) NOT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX          idx_bank_verify_code_account (bank_code, account_number)
);

-- =========================================
-- 6. [가상 금융망] 거래 기록 (출금/입금 + 타임아웃 시뮬레이션)
-- =========================================
CREATE TABLE bank_transaction
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    tran_seq_no    CHAR(36)    NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    type           VARCHAR(20) NOT NULL, -- CHARGE(출금) | WITHDRAW(입금)
    amount         BIGINT      NOT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_bank_transaction_tran_seq_no UNIQUE (tran_seq_no),
    CONSTRAINT chk_bank_transaction_amount CHECK (amount > 0)
);