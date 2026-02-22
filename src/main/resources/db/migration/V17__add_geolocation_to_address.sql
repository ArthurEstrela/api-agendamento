-- Adiciona as colunas de latitude e longitude na tabela onde o AddressVo é usado
-- Substitua 'service_providers' pela tabela correta se o AddressVo estiver a ser usado noutro lado (ex: users)

ALTER TABLE service_providers 
ADD COLUMN address_lat DOUBLE PRECISION,
ADD COLUMN address_lng DOUBLE PRECISION;