package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import com.stylo.api_agendamento.core.usecases.CreateProductUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Gestão de estoque, reposição e venda de produtos físicos")
public class ProductController {

    private final IProductRepository productRepository;
    private final CreateProductUseCase createProductUseCase;

    @Operation(summary = "Listar produtos ativos do salão", description = "Endpoint público/aberto para listar os produtos de um estabelecimento na página de agendamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de produtos recuperada com sucesso")
    })
    @GetMapping("/provider/{providerId}")
    // Rota pública (permitAll no SecurityConfig) pois clientes precisam ver os produtos para agendar/comprar
    public ResponseEntity<List<Product>> getActiveProducts(@PathVariable UUID providerId) {
        // ✨ CORREÇÃO: Utilizando o método exato da sua interface IProductRepository e passando o UUID direto
        List<Product> products = productRepository.findAllActiveByProviderId(providerId); 
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Criar novo produto", description = "Adiciona um produto ao estoque com preços de custo, venda e alertas de estoque mínimo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: preço negativo)")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Product> createProduct(@RequestBody @Valid CreateProductRequest request) {
        
        // O providerId é extraído com segurança internamente no UseCase através da interface IUserContext
        // Isso impede a injeção de produtos no estoque de terceiros.
        
        var input = new CreateProductUseCase.Input(
                request.name(),
                request.description(),
                request.salePrice(),
                request.costPrice(),
                request.initialStock(),
                request.minStockAlert()
        );

        Product createdProduct = createProductUseCase.execute(input);

        // Gera o cabeçalho Location (Boas práticas REST)
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProduct.getId())
                .toUri();

        return ResponseEntity.created(uri).body(createdProduct);
    }

    // --- DTO Atualizado (Reflete a nova riqueza do domínio) ---

    public record CreateProductRequest(
            @NotBlank(message = "O nome do produto é obrigatório")
            String name,

            String description,

            @NotNull(message = "O preço de venda é obrigatório")
            @PositiveOrZero(message = "O preço de venda não pode ser negativo")
            BigDecimal salePrice, // Renomeado para alinhar com o domínio

            @PositiveOrZero(message = "O preço de custo não pode ser negativo")
            BigDecimal costPrice, // NOVO: Permite calcular lucro

            @NotNull(message = "A quantidade inicial é obrigatória")
            @Min(value = 0, message = "O estoque não pode ser negativo")
            Integer initialStock, // Renomeado para alinhar com o domínio

            @Min(value = 0, message = "O alerta de estoque mínimo não pode ser negativo")
            Integer minStockAlert // NOVO: Gatilho para notificação
    ) {}
}