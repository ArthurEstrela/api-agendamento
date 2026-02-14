package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class Payout {
    private String id;
    private String professionalId;
    private String serviceProviderId;
    private BigDecimal totalAmount;
    private List<String> appointmentIds; // IDs consolidados neste pagamento
    private LocalDateTime processedAt;
    private String status; // "PAID", "PENDING"
}