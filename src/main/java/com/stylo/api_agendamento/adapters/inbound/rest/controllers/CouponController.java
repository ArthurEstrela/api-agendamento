package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.coupon.DiscountType;
import com.stylo.api_agendamento.core.usecases.ApplyCouponUseCase;
import com.stylo.api_agendamento.core.usecases.ManageCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Cupões", description = "Gestão de descontos e promoções do estabelecimento")
public class CouponController {

    private final ManageCouponUseCase manageCouponUseCase;
    private final ApplyCouponUseCase applyCouponUseCase;

    @Operation(summary = "Criar Cupão (Staff)", description = "Cria um novo código promocional exclusivo para o estabelecimento logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cupão criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou código de cupão já existente neste estabelecimento")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Coupon> create(@RequestBody @Valid CreateCouponRequest request) {
        // O UseCase já extrai o utilizador e o providerId internamente.
        // Asseguramos agora que todos os 6 parâmetros do domínio são passados corretamente.
        Coupon createdCoupon = manageCouponUseCase.create(
                request.code(),
                request.type(),
                request.value(),
                request.expirationDate(),
                request.maxUsages(),
                request.minPurchaseValue()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCoupon);
    }

    @Operation(summary = "Desativar Cupão (Staff)", description = "Desativa um cupão existente para impedir novas utilizações.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cupão desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: Cupão pertence a outro estabelecimento"),
            @ApiResponse(responseCode = "404", description = "Cupão não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        // Rota essencial que faltava expor para o Front-end gerir os descontos
        manageCouponUseCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Validar Cupão (Checkout)", description = "Simula a aplicação do cupão para exibir o desconto e o valor final no ecrã de pagamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cupão válido, retorna os cálculos do desconto"),
            @ApiResponse(responseCode = "400", description = "Cupão inválido, esgotado, expirado ou valor mínimo não atingido")
    })
    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()") // Clientes precisam de estar autenticados para simular descontos
    public ResponseEntity<CouponValidationResponse> validate(
            @RequestParam @NotBlank String code,
            @RequestParam @NotNull UUID providerId, // Corrigido de String para UUID
            @RequestParam @NotNull @PositiveOrZero BigDecimal amount) {
        
        // Passamos o providerId explícito porque o cliente pode estar a tentar agendar em vários salões diferentes
        var result = applyCouponUseCase.validateAndCalculate(code, providerId, amount);
        
        return ResponseEntity.ok(new CouponValidationResponse(
                true,
                result.discountAmount(),
                amount.subtract(result.discountAmount()) // Subtração feita de forma segura com BigDecimal
        ));
    }

    // --- DTOs Internos ---

    public record CreateCouponRequest(
            @NotBlank(message = "O código do cupão não pode estar vazio") 
            String code,
            
            @NotNull(message = "O tipo de desconto (percentagem ou fixo) é obrigatório") 
            DiscountType type,
            
            @NotNull(message = "O valor do desconto é obrigatório") 
            @PositiveOrZero(message = "O valor não pode ser negativo") 
            BigDecimal value,
            
            LocalDate expirationDate,
            
            @PositiveOrZero(message = "O limite de utilizações não pode ser negativo") 
            Integer maxUsages, // Adicionado para cumprir o UseCase
            
            @PositiveOrZero(message = "O valor mínimo de compra não pode ser negativo") 
            BigDecimal minPurchaseValue // Adicionado para cumprir o UseCase
    ) {}

    public record CouponValidationResponse(
            boolean valid,
            BigDecimal discount,
            BigDecimal finalPrice
    ) {}
}