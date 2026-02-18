CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL, -- Ex: 'Professional', 'Appointment'
    entity_id VARCHAR(36) NOT NULL,   -- ID do registro alterado
    action VARCHAR(20) NOT NULL,      -- CREATE, UPDATE, DELETE
    field_name VARCHAR(50),           -- Ex: 'commissionPercentage'
    old_value TEXT,                   -- Valor antigo (JSON ou String)
    new_value TEXT,                   -- Valor novo
    modified_by VARCHAR(36),          -- ID do usuário que fez a ação
    modified_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45)            -- Opcional: IP de quem mudou
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_modified_by ON audit_logs(modified_by);