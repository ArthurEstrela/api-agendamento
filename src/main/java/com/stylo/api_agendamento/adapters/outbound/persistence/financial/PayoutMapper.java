package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.core.domain.Payout;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayoutMapper {

    PayoutEntity toEntity(Payout domain);

    Payout toDomain(PayoutEntity entity);
}