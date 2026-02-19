package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ApplyCouponUseCase {

    private final ICouponRepository couponRepository;

    /**
     * Valida a aplicabilidade de um cupom e calcula o valor do desconto.
     * * @param code O código de texto do cupom (ex: BEMVINDO10)
     * @param providerId O ID do estabelecimento onde o cupom está sendo aplicado
     * @param purchaseAmount O valor total da compra/agendamento antes do desconto
     * @return CouponResult contendo a entidade do cupom e o valor calculado do desconto
     */
    public CouponResult validateAndCalculate(String code, UUID providerId, BigDecimal purchaseAmount) {
        // 1. Tratamento de entrada nula ou vazia
        if (code == null || code.isBlank()) {
            return new CouponResult(null, BigDecimal.ZERO);
        }

        // 2. Busca o cupom no repositório com o código normalizado
        // O isolamento por providerId garante o multi-tenancy
        Coupon coupon = couponRepository.findByCodeAndProviderId(code.trim().toUpperCase(), providerId)
                .orElseThrow(() -> new BusinessException("Cupom inválido ou não encontrado para este estabelecimento."));

        try {
            // 3. Calcula o desconto usando a lógica rica do domínio
            // O método calculateDiscount já chama internamente validateUsage()
            // Isso valida: status ativo, data de expiração, limite de usos e valor mínimo.
            BigDecimal discountAmount = coupon.calculateDiscount(purchaseAmount);

            log.info("Cupom {} aplicado com sucesso. Desconto: R$ {}", coupon.getCode(), discountAmount);
            return new CouponResult(coupon, discountAmount);

        } catch (BusinessException e) {
            // Re-lança exceções de negócio vindas do domínio (ex: "Cupom expirado")
            log.warn("Falha ao aplicar cupom {}: {}", code, e.getMessage());
            throw e;
        }
    }

    /**
     * DTO para transporte do resultado do cálculo.
     */
    public record CouponResult(
            Coupon coupon, 
            BigDecimal discountAmount
    ) {}
}