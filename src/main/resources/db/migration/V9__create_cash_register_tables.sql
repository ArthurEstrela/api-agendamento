CREATE TABLE cash_registers (
    id VARCHAR(36) PRIMARY KEY,
    provider_id VARCHAR(36) NOT NULL,
    open_time TIMESTAMP NOT NULL,
    close_time TIMESTAMP,
    initial_balance DECIMAL(19, 2) NOT NULL,
    final_balance DECIMAL(19, 2),
    calculated_balance DECIMAL(19, 2) NOT NULL,
    is_open BOOLEAN NOT NULL,
    opened_by_user_id VARCHAR(36) NOT NULL,
    closed_by_user_id VARCHAR(36)
);

CREATE TABLE cash_transactions (
    id VARCHAR(36) PRIMARY KEY,
    cash_register_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    description VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    performed_by_user_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_cash_register FOREIGN KEY (cash_register_id) REFERENCES cash_registers(id)
);

CREATE INDEX idx_cash_register_provider_open ON cash_registers(provider_id, is_open);