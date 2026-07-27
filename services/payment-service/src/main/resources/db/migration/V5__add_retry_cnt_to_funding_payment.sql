ALTER TABLE funding_payment
    ADD COLUMN retry_cnt INT NOT NULL DEFAULT 0;