package com.stylo.api_agendamento.adapters.outbound.persistence.review;

import com.stylo.api_agendamento.core.domain.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review toDomain(ReviewEntity entity);

    ReviewEntity toEntity(Review domain);
}