CREATE TABLE chat_room
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    creator_id BIGINT                              NOT NULL,
    product_id BIGINT                              NOT NULL,
    title      VARCHAR(100),
    status     VARCHAR(20)                         NOT NULL,
    type       VARCHAR(20)                         NOT NULL,
    created_at TIMESTAMP                           NOT NULL,
    deleted_at TIMESTAMP,

    CONSTRAINT PK_CHAT_ROOM PRIMARY KEY (id)
);

CREATE TABLE chat_message
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    room_id    BIGINT                              NOT NULL,
    sender_id  BIGINT                              NOT NULL,
    content    TEXT                                NOT NULL,
    status     VARCHAR(20)                         NOT NULL,
    created_at TIMESTAMP                           NOT NULL,
    deleted_at TIMESTAMP,
    client_id  VARCHAR(100),

    CONSTRAINT PK_CHAT_MESSAGE PRIMARY KEY (id),
    CONSTRAINT FK_CHAT_MESSAGE_ROOM FOREIGN KEY (room_id) REFERENCES chat_room (id)
);

CREATE TABLE inquiry_chat_member
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    room_id             BIGINT                              NOT NULL,
    member_id           BIGINT                              NOT NULL,
    status              VARCHAR(20)                         NOT NULL,
    last_read_message_id BIGINT,
    joined_at           TIMESTAMP                           NOT NULL,
    updated_at          TIMESTAMP,
    deleted_at          TIMESTAMP,

    CONSTRAINT PK_INQUIRY_CHAT_MEMBER PRIMARY KEY (id),
    CONSTRAINT FK_INQUIRY_CHAT_MEMBER_ROOM FOREIGN KEY (room_id) REFERENCES chat_room (id),
    CONSTRAINT FK_INQUIRY_CHAT_MEMBER_LAST_MSG FOREIGN KEY (last_read_message_id) REFERENCES chat_message (id)
);

CREATE TABLE funding_chat_blacklist
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    room_id    BIGINT                              NOT NULL,
    member_id  BIGINT                              NOT NULL,
    reason     VARCHAR(255),
    status     VARCHAR(20)                         NOT NULL,
    banned_at  TIMESTAMP                           NOT NULL,
    deleted_at TIMESTAMP,

    CONSTRAINT PK_FUNDING_CHAT_BLACKLIST PRIMARY KEY (id),
    CONSTRAINT FK_FUNDING_CHAT_BLACKLIST_ROOM FOREIGN KEY (room_id) REFERENCES chat_room (id)
);

CREATE INDEX idx_chat_message_room_id ON chat_message (room_id);
CREATE INDEX idx_chat_message_room_created ON chat_message (room_id, created_at);
CREATE UNIQUE INDEX idx_chat_message_client_id ON chat_message (client_id) WHERE client_id IS NOT NULL;

CREATE INDEX idx_inquiry_member_room ON inquiry_chat_member (room_id);
CREATE INDEX idx_inquiry_member_member ON inquiry_chat_member (member_id);

CREATE INDEX idx_blacklist_room ON funding_chat_blacklist (room_id);
CREATE INDEX idx_blacklist_member ON funding_chat_blacklist (member_id);
