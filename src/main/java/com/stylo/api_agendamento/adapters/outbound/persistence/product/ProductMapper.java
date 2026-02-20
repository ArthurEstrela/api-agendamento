package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import com.stylo.api_agendamento.core.domain.Product;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    Product toDomain(ProductEntity entity);

    ProductEntity toEntity(Product domain);
}