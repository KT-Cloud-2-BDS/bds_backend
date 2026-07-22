CREATE TABLE event_publication
(
    id                     BINARY(16)   NOT NULL,
    listener_id            VARCHAR(255) NOT NULL,
    event_type             VARCHAR(255) NOT NULL,
    serialized_event       TEXT         NOT NULL,
    publication_date       DATETIME(6)  NOT NULL,
    completion_date        DATETIME(6),
    last_resubmission_date DATETIME(6),
    completion_attempts    INT          NOT NULL DEFAULT 0,
    status                 VARCHAR(255) NOT NULL,

    CONSTRAINT pk_event_publication PRIMARY KEY (id),
    CONSTRAINT ck_event_publication_status
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

CREATE INDEX idx_event_publication_status ON event_publication (status);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);