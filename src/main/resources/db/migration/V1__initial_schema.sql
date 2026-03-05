CREATE TABLE
    users (
        id UUID PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        email VARCHAR(150) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        role VARCHAR(30) NOT NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT NOW (),
        updated_at TIMESTAMP
    );

CREATE TABLE
    service_providers (
        id UUID PRIMARY KEY,
        business_name VARCHAR(150) NOT NULL,
        owner_email VARCHAR(100) NOT NULL,
        document_number VARCHAR(20),
        document_type VARCHAR(10),
        business_phone VARCHAR(20),
        public_profile_slug VARCHAR(100) NOT NULL UNIQUE,
        logo_url TEXT,
        banner_url TEXT,
        subscription_status VARCHAR(20) NOT NULL,
        trial_ends_at TIMESTAMP,
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT NOW (),
        updated_at TIMESTAMP,
        address_street VARCHAR(255),
        address_number VARCHAR(50),
        address_neighborhood VARCHAR(150),
        address_city VARCHAR(100),
        address_state VARCHAR(50),
        address_zip_code VARCHAR(20) -- <-- VÍRGULA REMOVIDA AQUI (A última coluna não pode ter vírgula)
    );

CREATE INDEX idx_provider_slug ON service_providers (public_profile_slug);

CREATE TABLE
    professionals (
        id UUID PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        email VARCHAR(100),
        phone VARCHAR(20),
        provider_id UUID NOT NULL REFERENCES service_providers (id),
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT NOW (),
        updated_at TIMESTAMP
    );

CREATE TABLE
    services (
        id UUID PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description TEXT,
        price DECIMAL(10, 2) NOT NULL,
        duration_minutes INTEGER NOT NULL,
        provider_id UUID NOT NULL REFERENCES service_providers (id),
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT NOW ()
    );

CREATE TABLE
    clients (
        id UUID PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        email VARCHAR(150),
        phone VARCHAR(20),
        provider_id UUID REFERENCES service_providers (id),
        created_at TIMESTAMP NOT NULL DEFAULT NOW ()
    );