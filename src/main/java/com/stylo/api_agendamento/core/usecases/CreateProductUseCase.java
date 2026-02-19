package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final IProductRepository productRepository;
    private final IUserContext userContext; // ✨ Para garantir segurança de Tenant

    @Transactional
    public Product execute(Input input) {
        // SEGURANÇA: O providerId deve vir do contexto do usuário logado 
        // para evitar que um usuário crie produtos para outro salão.
        UUID providerId = userContext.getCurrentUser().getProviderId();

        log.info("Cadastrando novo produto: {} para o estabelecimento {}", input.name(), providerId);

        // Chamada corrigida para a Factory de Domínio
        Product product = Product.create(
                providerId,
                input.name(),
                input.description(),
                input.salePrice(),
                input.costPrice(), // ✨ Agora o UseCase supre a necessidade do domínio
                input.initialStock(),
                input.minStockAlert()
        );

        Product savedProduct = productRepository.save(product);
        
        log.info("✅ Produto '{}' (ID: {}) criado com sucesso.", 
                savedProduct.getName(), savedProduct.getId());
        
        return savedProduct;
    }

    /**
     * Input Record completo para um gerenciamento de estoque profissional.
     */
    public record Input(
            String name,
            String description,
            BigDecimal salePrice,    // Preço de venda ao cliente
            BigDecimal costPrice,    // Preço de compra/custo
            Integer initialStock,
            Integer minStockAlert    // Gatilho para notificação de reposição
    ) {}
}