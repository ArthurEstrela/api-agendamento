package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.coupon.DiscountType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICouponRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ManageCouponUseCase {

    private final ICouponRepository couponRepository;
    private final IUserContext userContext;

    @Transactional
    public Coupon create(String code, DiscountType type, BigDecimal value, LocalDate expiration, 
                         Integer maxUsages, BigDecimal minPurchaseValue) {
        
        User currentUser = userContext.getCurrentUser();
        UUID providerId = currentUser.getProviderId();

        // 1. Normalização e Verificação de Duplicidade dentro do estabelecimento
        String normalizedCode = code.trim().toUpperCase();
        if (couponRepository.findByCodeAndProviderId(normalizedCode, providerId).isPresent()) {
            throw new BusinessException("Já existe um cupom ativo com o código '" + normalizedCode + "' neste estabelecimento.");
        }

        // 2. Criação via Factory Method do Domínio (Garante integridade)
        Coupon coupon = Coupon.create(
                providerId,
                normalizedCode,
                type,
                value,
                expiration,
                maxUsages,
                minPurchaseValue
        );

        log.info("Novo cupom '{}' criado para o estabelecimento {}.", normalizedCode, providerId);
        return couponRepository.save(coupon);
    }

    @Transactional
    public void deactivate(UUID couponId) {
        UUID providerId = userContext.getCurrentUser().getProviderId();
        
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException("Cupom não encontrado."));

        if (!coupon.getProviderId().equals(providerId)) {
            throw new BusinessException("Acesso negated: este cupom pertence a outro estabelecimento.");
        }

        coupon.deactivate();
        couponRepository.save(coupon);
    }
}