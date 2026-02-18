package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.coupon.DiscountType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICouponRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class ManageCouponUseCase {

    private final ICouponRepository couponRepository;
    private final IUserContext userContext;

    public Coupon create(String code, DiscountType type, java.math.BigDecimal value, java.time.LocalDate expiration) {
        User user = userContext.getCurrentUser();
        
        // Verifica duplicidade no mesmo estabelecimento
        if (couponRepository.findByCodeAndProvider(code.toUpperCase(), user.getProviderId()).isPresent()) {
            throw new BusinessException("Já existe um cupom com este código.");
        }

        Coupon coupon = Coupon.builder()
                .id(UUID.randomUUID().toString())
                .providerId(user.getProviderId())
                .code(code.toUpperCase()) // Padronizar UpperCase
                .type(type)
                .value(value)
                .expirationDate(expiration)
                .currentUsages(0)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return couponRepository.save(coupon);
    }
    
    // Método para listar cupons do estabelecimento, desativar, etc...
}