CREATE TABLE
    coupons (
        id VARCHAR(36) PRIMARY KEY,
        provider_id VARCHAR(36) NOT NULL,
        code VARCHAR(50) NOT NULL,
        type VARCHAR(20) NOT NULL,
        value DECIMAL(19, 2) NOT NULL,
        expiration_date DATE,
        max_usages INTEGER,
        current_usages INTEGER DEFAULT 0,
        min_purchase_value DECIMAL(19, 2),
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL,
        CONSTRAINT uq_coupon_code_provider UNIQUE (provider_id, code)
    );

CREATE INDEX idx_coupon_code ON coupons (code);

CREATE INDEX idx_coupon_provider ON coupons (provider_id);

-- ⚠️ IMPORTANTE: Certifique-se que estas linhas estão no arquivo V10 --
ALTER TABLE appointments
ADD COLUMN coupon_id VARCHAR(36);

ALTER TABLE appointments
ADD COLUMN discount_amount DECIMAL(19, 2) DEFAULT 0;

ALTER TABLE appointments
ADD COLUMN final_price DECIMAL(19, 2);

ALTER TABLE appointments
ADD COLUMN professional_commission DECIMAL(19, 2);

ALTER TABLE appointments
ADD COLUMN service_provider_fee DECIMAL(19, 2);

ALTER TABLE appointments
ADD COLUMN commission_settled BOOLEAN DEFAULT FALSE;

ALTER TABLE appointments ADD CONSTRAINT fk_appointment_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id);