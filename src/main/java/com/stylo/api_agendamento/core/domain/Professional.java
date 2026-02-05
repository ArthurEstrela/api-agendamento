package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Professional {
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    private String bio;
    private List<Service> services;
    private List<DailyAvailability> availability;
    private Integer slotInterval;
    private String serviceProviderId;
    private boolean isOwner;
}