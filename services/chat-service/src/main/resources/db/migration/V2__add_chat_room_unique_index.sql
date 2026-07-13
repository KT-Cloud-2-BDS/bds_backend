CREATE UNIQUE INDEX uq_inquiry_room ON chat_room (creator_id, product_id) WHERE type = 'INQUIRY' AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_funding_room ON chat_room (product_id) WHERE type = 'FUNDING' AND deleted_at IS NULL;
