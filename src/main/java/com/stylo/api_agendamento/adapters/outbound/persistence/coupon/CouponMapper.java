package com.stylo.api_agendamento.adapters.outbound.persistence.coupon;

import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    Coupon toDomain(CouponEntity entity);

    // Define o valor padrão de 0 para currentUsages caso seja null no domínio
    @Mapping(target = "currentUsages", source = "currentUsages", defaultValue = "0")
    CouponEntity toEntity(Coupon domain);
}