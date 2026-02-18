ALTER TABLE users ADD COLUMN provider_id UUID;

-- Cria índice para deixar o login rápido
CREATE INDEX idx_users_provider_id ON users(provider_id);

-- Opcional: Integridade referencial (se quiser ser estrito)
ALTER TABLE users 
ADD CONSTRAINT fk_users_service_provider 
FOREIGN KEY (provider_id) REFERENCES service_providers(id);