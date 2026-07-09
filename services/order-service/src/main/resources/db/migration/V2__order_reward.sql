ALTER TABLE "order"
    ADD COLUMN cancelled_at TIMESTAMP NULL;
ALTER TABLE "order"
    DROP COLUMN amount;
ALTER TABLE "order"
    ADD COLUMN total_reward_amount BIGINT NOT NULL DEFAULT 0;
ALTER TABLE "order"
    ADD COLUMN total_shipping_charge BIGINT NOT NULL DEFAULT 0;
ALTER TABLE "order"
    ADD COLUMN expires_at TIMESTAMP NULL;

ALTER TABLE order_reward
    ADD COLUMN shipping_charge BIGINT NOT NULL DEFAULT 0;
ALTER TABLE order_reward
    ADD COLUMN amount BIGINT NOT NULL DEFAULT 0;
