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

CREATE INDEX idx_event_publication_status ON event_publication (status);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
