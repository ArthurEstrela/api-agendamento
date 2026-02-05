package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceProvider {
    private String id;
    private String businessName;
    private Address businessAddress; // Objeto de endereço completo
    private String cnpj;
    private String cpf;
    private String documentType; // "cpf" | "cnpj"
    private String businessPhone;
    private String publicProfileSlug; // Para a URL pública
    private String logoUrl;
    private String bannerUrl;
    private String pixKey; // Para recebimentos
    private String pixKeyType; // "email", "cpf", etc
    private List<String> paymentMethods; // ["pix", "credit_card", "cash"]
    private Integer cancellationMinHours; // Política de cancelamento
    private String subscriptionStatus; // Controle de acesso SaaS
}