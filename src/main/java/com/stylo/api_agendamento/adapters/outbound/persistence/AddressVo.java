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
public class AddressVo {

    @Column(name = "address_street", length = 150)
    private String street;

    @Column(name = "address_number", length = 20)
    private String number;

    @Column(name = "address_neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_state", length = 2) // Padrão UF (SP, GO, etc)
    private String state;

    @Column(name = "address_zip_code", length = 10)
    private String zipCode;
}