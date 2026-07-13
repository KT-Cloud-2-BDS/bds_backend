CREATE TABLE notifications
(
    notification_id BIGSERIAL PRIMARY KEY,
    member_id       BIGINT       NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    target_id       BIGINT       NOT NULL,
    title           VARCHAR(255) NOT NULL,
    body            TEXT         NOT NULL,
    channel         VARCHAR(20)  NOT NULL,
    send_status     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    read_at         TIMESTAMP,
    clicked_at      TIMESTAMP
);

CREATE TABLE notification_subscription
(
    subscription_id BIGSERIAL PRIMARY KEY,
    member_id       BIGINT      NOT NULL,
    target_type     VARCHAR(50) NOT NULL,
    target_id       BIGINT      NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP
);

CREATE UNIQUE INDEX uq_notification_subscription_active
    ON notification_subscription (member_id, target_type, target_id)
    WHERE is_deleted = false;

CREATE TABLE fcm_token
(
    fcm_token_id BIGSERIAL PRIMARY KEY,
    member_id    BIGINT       NOT NULL,
    token        VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);
