-- V2__change_tranSeqNo.sql

-- ============================================
-- 1. funding_payment
-- ============================================
ALTER TABLE funding_payment ADD COLUMN tran_seq_no_bin BINARY(16);
UPDATE funding_payment SET tran_seq_no_bin = UUID_TO_BIN(tran_seq_no, 1) WHERE tran_seq_no IS NOT NULL;
ALTER TABLE funding_payment DROP INDEX uq_funding_payment_tran_seq_no;
ALTER TABLE funding_payment DROP COLUMN tran_seq_no;
ALTER TABLE funding_payment CHANGE COLUMN tran_seq_no_bin tran_seq_no BINARY(16) NOT NULL;
ALTER TABLE funding_payment ADD CONSTRAINT uq_funding_payment_tran_seq_no UNIQUE (tran_seq_no);

-- ============================================
-- 2. payment_history
-- ============================================
ALTER TABLE payment_history ADD COLUMN tran_seq_no_bin BINARY(16);
UPDATE payment_history SET tran_seq_no_bin = UUID_TO_BIN(tran_seq_no, 1) WHERE tran_seq_no IS NOT NULL;
ALTER TABLE payment_history DROP INDEX uq_payment_history_tran_seq_no;
ALTER TABLE payment_history DROP COLUMN tran_seq_no;
ALTER TABLE payment_history CHANGE COLUMN tran_seq_no_bin tran_seq_no BINARY(16) NOT NULL;
ALTER TABLE payment_history ADD CONSTRAINT uq_payment_history_tran_seq_no UNIQUE (tran_seq_no);

-- ============================================
-- 3. bank_transaction
-- ============================================
ALTER TABLE bank_transaction ADD COLUMN tran_seq_no_bin BINARY(16);
UPDATE bank_transaction SET tran_seq_no_bin = UUID_TO_BIN(tran_seq_no, 1) WHERE tran_seq_no IS NOT NULL;
ALTER TABLE bank_transaction DROP INDEX uq_bank_transaction_tran_seq_no;
ALTER TABLE bank_transaction DROP COLUMN tran_seq_no;
ALTER TABLE bank_transaction CHANGE COLUMN tran_seq_no_bin tran_seq_no BINARY(16) NOT NULL;
ALTER TABLE bank_transaction ADD CONSTRAINT uq_bank_transaction_tran_seq_no UNIQUE (tran_seq_no);