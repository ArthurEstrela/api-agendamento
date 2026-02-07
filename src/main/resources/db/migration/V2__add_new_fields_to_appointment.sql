ALTER TABLE appointments ADD COLUMN client_phone VARCHAR(20);
ALTER TABLE appointments ADD COLUMN payment_method VARCHAR(30);
ALTER TABLE appointments ADD COLUMN total_duration INTEGER;
ALTER TABLE appointments ADD COLUMN notes TEXT;
ALTER TABLE appointments ADD COLUMN final_price DECIMAL(10,2);
ALTER TABLE appointments ADD COLUMN reminder_minutes INTEGER DEFAULT 0;
ALTER TABLE appointments ADD COLUMN notified BOOLEAN DEFAULT FALSE;
ALTER TABLE appointments ADD COLUMN is_personal_block BOOLEAN DEFAULT FALSE;