-- =============================================
-- V4: Spring Modulith Outbox (event_publication)
-- Spring Modulith 2.1.0 공식 스키마 기반
-- =============================================

CREATE TABLE event_publication
(
    id                     UUID                        NOT NULL,
    listener_id            TEXT                        NOT NULL,
    event_type             TEXT                        NOT NULL,
    serialized_event       TEXT                        NOT NULL,
    publication_date       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    completion_attempts    INTEGER,
    status                 TEXT,
    CONSTRAINT pk_event_publication PRIMARY KEY (id)
);

-- 미완료 이벤트 재발행 조회
CREATE INDEX idx_event_publication_status ON event_publication (status);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);