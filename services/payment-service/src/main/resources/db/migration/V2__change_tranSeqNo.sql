-- V2__change_tranSeqNo.sql

ALTER TABLE funding_payment MODIFY COLUMN tran_seq_no BINARY(16) NOT NULL;
ALTER TABLE payment_history MODIFY COLUMN tran_seq_no BINARY(16) NOT NULL;
ALTER TABLE bank_transaction MODIFY COLUMN tran_seq_no BINARY(16) NOT NULL;