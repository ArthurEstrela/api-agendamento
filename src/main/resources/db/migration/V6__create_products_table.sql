-- Tabela de Produtos
CREATE TABLE products (
    id VARCHAR(36) PRIMARY KEY,
    service_provider_id VARCHAR(36) NOT NULL REFERENCES service_providers(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela associativa: Quais produtos foram vendidos em qual agendamento (Comanda)
CREATE TABLE appointment_products (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id VARCHAR(36) NOT NULL REFERENCES appointments(id),
    product_id VARCHAR(36) NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL -- Salva o preço na hora da venda (caso mude depois)
);