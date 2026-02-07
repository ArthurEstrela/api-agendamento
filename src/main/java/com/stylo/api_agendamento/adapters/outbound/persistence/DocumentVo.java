package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentVo {
    @Column(name = "document_number", nullable = false)
    private String value; // O número do CPF ou CNPJ puro
    
    @Column(name = "document_type")
    private String type;  // "CPF" ou "CNPJ"
}