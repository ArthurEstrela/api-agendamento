ALTER TABLE service_providers
ADD COLUMN stripe_account_id VARCHAR(255), -- ID da conta conectada (acct_...)
ADD COLUMN stripe_customer_id VARCHAR(255), -- ID dele como pagador da mensalidade
ADD COLUMN online_payments_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN platform_fee_percentage DECIMAL(5, 2) DEFAULT 2.00;

ALTER TABLE appointments
ADD COLUMN settled_at TIMESTAMP; -- Data em que o dinheiro caiu na conta