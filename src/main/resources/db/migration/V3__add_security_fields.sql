ALTER TABLE users 
ADD COLUMN reset_password_token VARCHAR(255),
ADD COLUMN reset_password_expires_at TIMESTAMP,
ADD COLUMN fcm_token VARCHAR(255), -- Para notificações Mobile
ADD COLUMN profile_picture_url TEXT;

ALTER TABLE service_providers
ADD COLUMN cancellation_min_hours INTEGER DEFAULT 2,
ADD COLUMN max_no_shows_allowed INTEGER DEFAULT 3;