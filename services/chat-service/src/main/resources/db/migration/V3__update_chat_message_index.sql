DROP INDEX IF EXISTS idx_chat_message_room_created;

CREATE INDEX idx_chat_message_room_created ON chat_message (room_id, id DESC) WHERE deleted_at IS NULL;
