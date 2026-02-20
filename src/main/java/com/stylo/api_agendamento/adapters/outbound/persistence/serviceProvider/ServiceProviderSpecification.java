package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.core.usecases.dto.ProviderSearchCriteria;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class ServiceProviderSpecification {

    public static Specification<ServiceProviderEntity> build(ProviderSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ✨ Regra 1: Só retornar perfis que estão configurados como PÚBLICOS
            predicates.add(cb.isNotNull(root.get("publicProfileSlug")));

            // ✨ Otimização: Evitar retornos duplicados devido ao Join
            query.distinct(true);

            // Join com a tabela de serviços (Left Join para não sumir quem ainda não tem serviço, 
            // a não ser que o filtro de preço exija)
            Join<Object, Object> servicesJoin = root.join("services", JoinType.LEFT);

            // 🔍 FILTRO 1: Termo de Busca (Nome da Barbearia OU Nome do Serviço)
            if (criteria.searchTerm() != null && !criteria.searchTerm().isBlank()) {
                String pattern = "%" + criteria.searchTerm().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate serviceNameMatch = cb.like(cb.lower(servicesJoin.get("name")), pattern);
                predicates.add(cb.or(nameMatch, serviceNameMatch));
            }

            // 🏙️ FILTRO 2: Cidade (Localização no AddressVo embutido)
            if (criteria.city() != null && !criteria.city().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("address").get("city")), criteria.city().toLowerCase()));
            }

            // ⭐ FILTRO 3: Avaliação Mínima
            if (criteria.minRating() != null) {
                // Assumindo que você tem ou terá um campo averageRating na Entity
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), criteria.minRating())); 
            }

            // 💵 FILTRO 4: Preço (Faixa de Preço dos Serviços)
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(servicesJoin.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(servicesJoin.get("price"), criteria.maxPrice()));
            }

            // Concatena todos os filtros dinâmicos com um "AND"
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}