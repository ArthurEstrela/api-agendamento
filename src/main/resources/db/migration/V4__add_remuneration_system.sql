-- Campos no Profissional
ALTER TABLE professionals ADD COLUMN remuneration_type VARCHAR(30) DEFAULT 'COMMISSION';
ALTER TABLE professionals ADD COLUMN commission_rate DECIMAL(5,2);
ALTER TABLE professionals ADD COLUMN fixed_value DECIMAL(10,2);

-- Campos no Agendamento
ALTER TABLE appointments ADD COLUMN professional_commission DECIMAL(10,2);
ALTER TABLE appointments ADD COLUMN service_provider_fee DECIMAL(10,2);