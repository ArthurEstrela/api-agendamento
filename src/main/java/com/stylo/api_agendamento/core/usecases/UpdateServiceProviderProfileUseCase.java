package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.UpdateServiceProviderInput;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

@RequiredArgsConstructor
public class UpdateServiceProviderProfileUseCase {

    private final IServiceProviderRepository repository;

    public ServiceProvider execute(String providerId, UpdateServiceProviderInput input) {
        // 1. Recupera o e-mail do usuário autenticado no Token JWT
        String authenticatedUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Busca o estabelecimento e valida existência
        ServiceProvider provider = repository.findById(providerId)
                .orElseThrow(() -> new BusinessException("Estabelecimento não encontrado."));

        // 3. SEGURANÇA SAAS: Valida se o usuário logado é de fato o dono deste estabelecimento
        // O e-mail do dono é capturado durante o registro no RegisterServiceProviderUseCase
        if (!provider.getOwnerEmail().equals(authenticatedUserEmail)) {
            throw new BusinessException("Acesso negado: Você não tem permissão para alterar este perfil.");
        }

        // 4. Validação de Unicidade do Slug (Apenas se houver alteração)
        if (input.slug() != null && !input.slug().equals(provider.getPublicProfileSlug().value())) {
            boolean slugExists = repository.existsBySlug(input.slug());
            if (slugExists) {
                throw new BusinessException("Esta URL amigável já está sendo usada por outro estabelecimento.");
            }
            provider.updateSlug(new Slug(input.slug()));
        }

        // 5. Atualização atômica de dados básicos
        provider.updateProfile(
            input.name(), 
            input.phoneNumber(), 
            input.logoUrl()
        );

        // 6. Atualização de endereço usando o VO do domínio
        if (input.address() != null) {
            provider.updateAddress(input.address().toDomain()); 
        }

        // 7. Persistência dos dados atualizados
        return repository.save(provider);
    }
}