CREATE TABLE google_tokens (
    id UUID PRIMARY KEY,
    professional_id UUID UNIQUE NOT NULL REFERENCES professionals(id),
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    scope TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE google_sync_retries (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    retry_count INTEGER DEFAULT 0,
    last_attempt TIMESTAMP,
    status VARCHAR(20),
    error_message TEXT
);