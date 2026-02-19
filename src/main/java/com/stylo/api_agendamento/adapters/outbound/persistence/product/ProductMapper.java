package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import com.stylo.api_agendamento.core.domain.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toDomain(ProductEntity entity);

    ProductEntity toEntity(Product domain);
}