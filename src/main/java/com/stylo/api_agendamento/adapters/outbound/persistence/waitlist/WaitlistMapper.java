package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import com.stylo.api_agendamento.core.domain.Waitlist;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WaitlistMapper {

    public Waitlist toDomain(WaitlistEntity entity) {
        if (entity == null) return null;
        
        return Waitlist.builder()
                .id(entity.getId().toString())
                .professionalId(entity.getProfessionalId().toString())
                .clientId(entity.getClientId().toString())
                .clientName(entity.getClientName())
                .clientPhone(entity.getClientPhone())
                .clientEmail(entity.getClientEmail())
                .desiredDate(entity.getDesiredDate())
                .requestTime(entity.getRequestTime())
                .notified(entity.isNotified())
                .notifiedAt(entity.getNotifiedAt())
                .build();
    }

    public WaitlistEntity toEntity(Waitlist domain) {
        if (domain == null) return null;

        return WaitlistEntity.builder()
                .id(domain.getId() != null ? UUID.fromString(domain.getId()) : null)
                .professionalId(UUID.fromString(domain.getProfessionalId()))
                .clientId(UUID.fromString(domain.getClientId()))
                .clientName(domain.getClientName())
                .clientPhone(domain.getClientPhone())
                .clientEmail(domain.getClientEmail())
                .desiredDate(domain.getDesiredDate())
                .requestTime(domain.getRequestTime())
                .notified(domain.isNotified())
                .notifiedAt(domain.getNotifiedAt())
                .build();
    }
}