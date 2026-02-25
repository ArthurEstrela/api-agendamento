-- Adiciona coluna para armazenar o UID do Firebase (String)
ALTER TABLE users 
ADD COLUMN firebase_id VARCHAR(128) UNIQUE;

-- Cria um índice para deixar o login ultra rápido
CREATE INDEX idx_user_firebase_id ON users(firebase_id);