CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES service_providers(id),
    client_id UUID NOT NULL REFERENCES clients(id),
    professional_id UUID REFERENCES professionals(id),
    
    -- ✨ AJUSTE CRÍTICO: Garante que o banco armazene o ponto exato no tempo global
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, CONFIRMED, COMPLETED, CANCELLED
    
    total_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    
    external_payment_id VARCHAR(255),
    paid BOOLEAN DEFAULT FALSE,
    
    -- ✨ AJUSTE CRÍTICO: Mantém a criação precisa
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE appointment_items (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    service_id UUID NOT NULL REFERENCES services(id),
    price_at_moment DECIMAL(10, 2) NOT NULL
);

-- Índices para performance
CREATE INDEX idx_appointments_date ON appointments(start_time);
CREATE INDEX idx_appointments_provider ON appointments(provider_id);