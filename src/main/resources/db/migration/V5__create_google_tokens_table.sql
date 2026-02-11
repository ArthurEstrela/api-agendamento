CREATE TABLE
    professional_google_tokens (
        professional_id UUID PRIMARY KEY,
        access_token TEXT NOT NULL,
        refresh_token TEXT NOT NULL,
        expires_at TIMESTAMP NOT NULL,
        resource_id VARCHAR(255),
        CONSTRAINT fk_professional FOREIGN KEY (professional_id) REFERENCES professionals (id) ON DELETE CASCADE
    );

ALTER TABLE appointments
ADD COLUMN external_event_id VARCHAR(255);