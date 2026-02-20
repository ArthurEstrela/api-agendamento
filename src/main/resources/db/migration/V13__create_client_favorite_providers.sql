CREATE TABLE client_favorite_providers (
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES service_providers(id) ON DELETE CASCADE,
    PRIMARY KEY (client_id, provider_id)
);

CREATE INDEX idx_favorite_providers_client ON client_favorite_providers(client_id);