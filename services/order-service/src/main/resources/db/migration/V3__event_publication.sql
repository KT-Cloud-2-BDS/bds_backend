-- =============================================
-- V3: Spring Modulith Outbox (event_publication)
-- =============================================

CREATE TABLE event_publication
(
    id                     UUID                     NOT NULL,
    listener_id            VARCHAR(255)             NOT NULL,
    event_type             VARCHAR(255)             NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    completion_attempts    INTEGER                  NOT NULL DEFAULT 0,
    status                 VARCHAR(255)             NOT NULL
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED')),

    CONSTRAINT pk_event_publication PRIMARY KEY (id)
);

-- 미완료 이벤트 재발행 조회 (modules/messaging의 BdsEventPublicationAutoConfig 스케줄러용)
CREATE INDEX idx_event_publication_status ON event_publication (status);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);
