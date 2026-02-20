ALTER TABLE service_providers 
ADD COLUMN average_rating NUMERIC(3, 2) DEFAULT 0.0,
ADD COLUMN total_reviews INTEGER DEFAULT 0;

-- Index opcional para deixar a busca por "Melhores Avaliados" ultra-rápida
CREATE INDEX idx_provider_rating ON service_providers(average_rating DESC);