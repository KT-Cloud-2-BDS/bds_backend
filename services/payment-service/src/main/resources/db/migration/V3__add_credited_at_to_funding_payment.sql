ALTER TABLE funding_payment
ADD COLUMN credited_at DATETIME(6) NULL
COMMENT '창작자 크레딧 완료 시각';

CREATE INDEX idx_funding_payment_credit_lookup
ON funding_payment (product_id, status, credited_at);