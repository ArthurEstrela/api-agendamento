-- Adiciona os vínculos de Perfil à tabela de Autenticação (Users)
ALTER TABLE users
ADD COLUMN professional_id UUID,
ADD COLUMN client_id UUID;

-- Opcional, mas recomendado para performance, já que você fará buscas por essas colunas:
CREATE INDEX idx_user_professional ON users(professional_id);
CREATE INDEX idx_user_client ON users(client_id);