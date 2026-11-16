-- Execute este SQL somente no banco saldo_db JA INICIALIZADO pela Saldo API.
-- Mantem app_users/accounts intactas. Hibernate UUID binario no MySQL.
CREATE TABLE IF NOT EXISTS pix_keys (
  id BINARY(16) NOT NULL,
  account_id BINARY(16) NOT NULL,
  key_type VARCHAR(12) NOT NULL,
  key_hash VARCHAR(64) NOT NULL,
  key_masked VARCHAR(200) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pix_keys_hash (key_hash),
  KEY idx_pix_keys_account (account_id),
  CONSTRAINT fk_pix_keys_account FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB;
