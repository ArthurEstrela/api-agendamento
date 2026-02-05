ALTER TABLE appointments ADD COLUMN client_phone VARCHAR(20);
ALTER TABLE appointments ADD COLUMN payment_method VARCHAR(30);
ALTER TABLE appointments ADD COLUMN total_duration INTEGER;
ALTER TABLE appointments ADD COLUMN notes TEXT;
ALTER TABLE appointments ADD COLUMN final_price DECIMAL(10,2);