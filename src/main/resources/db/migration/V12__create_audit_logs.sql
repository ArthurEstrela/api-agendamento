CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    entity_name VARCHAR(255) NOT NULL, -- ✨ Ajustado para 255
    entity_id UUID NOT NULL,           -- ✨ ALTERADO PARA UUID
    action VARCHAR(20) NOT NULL,      
    field_name VARCHAR(255),           -- ✨ Ajustado para 255
    old_value TEXT,                   
    new_value TEXT,                   
    modified_by UUID,                  -- ✨ ALTERADO PARA UUID
    modified_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(255),           -- ✨ Ajustado para 255
    provider_id UUID                   -- ✨ ADICIONADO (Hibernate estava pedindo)
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_modified_by ON audit_logs(modified_by);