package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import com.stylo.api_agendamento.core.usecases.CreateProductUseCase;
import com.stylo.api_agendamento.core.usecases.CreateProductUseCase.CreateProductInput;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.product.CreateProductRequest; 

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Gestão de estoque e venda de produtos")
public class ProductController {

    private final IProductRepository productRepository;
    private final CreateProductUseCase createProductUseCase;

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Listar produtos ativos", description = "Usado na página de agendamento público e no dashboard")
    public ResponseEntity<List<Product>> getActiveProducts(@PathVariable String providerId) {
        // CQRS simplificado: Consulta direta via repositório para leitura simples
        List<Product> products = productRepository.findByServiceProviderIdAndIsActiveTrue(providerId);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    @Operation(summary = "Criar novo produto", description = "Adiciona um produto ao estoque do estabelecimento")
    public ResponseEntity<Product> createProduct(
            @RequestBody @Valid CreateProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails // Pega o usuário logado para garantir segurança
    ) {
        // Assume que o ID do usuário logado é o providerId (ou você extrai do token)
        String providerId = userDetails.getUsername(); // Ajuste conforme sua lógica de UserDetails

        CreateProductInput input = new CreateProductInput(
                providerId,
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity()
        );

        Product createdProduct = createProductUseCase.execute(input);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProduct.getId())
                .toUri();

        return ResponseEntity.created(uri).body(createdProduct);
    }
}