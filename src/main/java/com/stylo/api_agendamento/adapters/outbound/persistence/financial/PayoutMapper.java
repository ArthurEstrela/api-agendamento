package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.core.domain.Payout;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PayoutMapper {

    public PayoutEntity toEntity(Payout domain) {
        return PayoutEntity.builder()
                .professionalId(UUID.fromString(domain.getProfessionalId()))
                .serviceProviderId(UUID.fromString(domain.getServiceProviderId()))
                .totalAmount(domain.getTotalAmount())
                .processedAt(domain.getProcessedAt())
                .status(domain.getStatus())
                .appointmentIds(domain.getAppointmentIds().stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList()))
                .build();
    }

    public Payout toDomain(PayoutEntity entity) {
        return Payout.builder()
                .id(entity.getId().toString())
                .professionalId(entity.getProfessionalId().toString())
                .serviceProviderId(entity.getServiceProviderId().toString())
                .totalAmount(entity.getTotalAmount())
                .processedAt(entity.getProcessedAt())
                .status(entity.getStatus())
                .appointmentIds(entity.getAppointmentIds().stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()))
                .build();
    }
}