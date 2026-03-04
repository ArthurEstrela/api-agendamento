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
public class SocialLinksVo {

    @Column(name = "social_instagram", length = 255)
    private String instagram;

    @Column(name = "social_facebook", length = 255)
    private String facebook;

    @Column(name = "social_website", length = 255)
    private String website;

    @Column(name = "social_whatsapp", length = 20)
    private String whatsapp;
}