-- =============================================
-- V1: Initial Schema
-- =============================================

-- Funding
CREATE TABLE funding
(
    id                BIGINT       NOT NULL,
    title             VARCHAR(100) NOT NULL,
    creator_id        BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    start_at          TIMESTAMP    NOT NULL,
    hold_to           TIMESTAMP    NOT NULL,
    pay_at            TIMESTAMP    NOT NULL,
    participation_cnt INT          NOT NULL DEFAULT 0,
    goal_amount       BIGINT       NOT NULL,
    current_amount    BIGINT       NOT NULL DEFAULT 0,
    is_success        BOOLEAN      NULL     DEFAULT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_funding PRIMARY KEY (id)
);

-- Reward
CREATE TABLE reward
(
    id              BIGINT       NOT NULL,
    funding_id      BIGINT       NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    description     VARCHAR(200) NULL,
    limit_qty       INT          NOT NULL,
    remain_qty      INT          NULL,
    badge_type      VARCHAR(20)  NULL     DEFAULT NULL,
    price           INT          NOT NULL,
    offer_at        TIMESTAMP    NOT NULL,
    shipping_charge INT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_reward PRIMARY KEY (id),
    CONSTRAINT fk_reward_funding FOREIGN KEY (funding_id) REFERENCES funding (id)
);

-- Order
CREATE TABLE "order"
(
    id            BIGINT      NOT NULL,
    member_id     BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount        INT         NULL,
    cancel_reason VARCHAR(20) NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NULL,

    CONSTRAINT pk_order PRIMARY KEY (id)
);

-- Order Reward
CREATE TABLE order_reward
(
    id        BIGINT NOT NULL,
    order_id  BIGINT NOT NULL,
    reward_id BIGINT NOT NULL,
    qty       INT    NOT NULL,

    CONSTRAINT pk_order_reward PRIMARY KEY (id),
    CONSTRAINT fk_order_reward_order FOREIGN KEY (order_id) REFERENCES "order" (id),
    CONSTRAINT fk_order_reward_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
);

-- =============================================
-- Indexes
-- =============================================
CREATE INDEX idx_funding_status ON funding (status);

CREATE INDEX idx_reward_funding_id ON reward (funding_id);

CREATE INDEX idx_order_status ON "order" (status);

CREATE INDEX idx_order_reward_order_id ON order_reward (order_id);
CREATE INDEX idx_order_reward_reward_id ON order_reward (reward_id);