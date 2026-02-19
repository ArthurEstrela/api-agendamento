package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class DocumentVo {

    @Column(name = "document_number", nullable = false, length = 20)
    private String value; 
    
    @Column(name = "document_type", length = 10)
    private String type; // CPF ou CNPJ
}