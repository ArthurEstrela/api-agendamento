ALTER TABLE professionals
ADD COLUMN commission_percentage DECIMAL(5, 2) DEFAULT 0.00,
ADD COLUMN remuneration_type VARCHAR(20) DEFAULT 'PERCENTAGE'; -- PERCENTAGE, FIXED, NONE

ALTER TABLE service_providers
ADD COLUMN commissions_enabled BOOLEAN DEFAULT FALSE;