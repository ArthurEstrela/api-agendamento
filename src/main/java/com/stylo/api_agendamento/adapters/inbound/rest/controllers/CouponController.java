package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.context.SpringUserContext;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.coupon.DiscountType;
import com.stylo.api_agendamento.core.usecases.ApplyCouponUseCase;
import com.stylo.api_agendamento.core.usecases.ManageCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Cupons", description = "Gestão de descontos")
public class CouponController {

    private final ManageCouponUseCase manageCouponUseCase;
    private final ApplyCouponUseCase applyCouponUseCase;
    private final SpringUserContext userContext; // Para pegar providerId na validação de teste

    @PostMapping
    @Operation(summary = "Criar Cupom (Provider)", description = "Cria um novo código promocional")
    public ResponseEntity<Coupon> create(@RequestBody @Valid CreateCouponRequest request) {
        // Segurança: Adicionar verificação de permissão no SecurityConfig ou aqui
        return ResponseEntity.ok(manageCouponUseCase.create(
                request.code(),
                request.type(),
                request.value(),
                request.expirationDate()
        ));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validar Cupom (Checkout)", description = "Simula a aplicação do cupom para mostrar o desconto no front")
    public ResponseEntity<CouponValidationResponse> validate(
            @RequestParam String code,
            @RequestParam String providerId,
            @RequestParam BigDecimal amount) {
        
        var result = applyCouponUseCase.validateAndCalculate(code, providerId, amount);
        
        return ResponseEntity.ok(new CouponValidationResponse(
                true,
                result.discountAmount(),
                amount.subtract(result.discountAmount())
        ));
    }

    public record CreateCouponRequest(
            @NotNull String code,
            @NotNull DiscountType type,
            @NotNull BigDecimal value,
            LocalDate expirationDate
    ) {}

    public record CouponValidationResponse(
            boolean valid,
            BigDecimal discount,
            BigDecimal finalPrice
    ) {}
}