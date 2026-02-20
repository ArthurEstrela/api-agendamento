CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL, -- SALE, INTERNAL_USE, RESTOCK
    quantity INTEGER NOT NULL,
    reason VARCHAR(255),
    performed_by_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_movement_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
-- Atualização caso a tabela products V6 precise de ajuste para cost_price
ALTER TABLE products ADD COLUMN IF NOT EXISTS cost_price DECIMAL(10, 2);