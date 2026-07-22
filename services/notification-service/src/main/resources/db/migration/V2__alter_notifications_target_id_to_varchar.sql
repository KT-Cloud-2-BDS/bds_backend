ALTER TABLE notifications
    ALTER COLUMN target_id TYPE VARCHAR(255) USING target_id::VARCHAR;
