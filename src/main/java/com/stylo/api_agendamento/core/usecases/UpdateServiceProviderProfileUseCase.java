package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpdateServiceProviderProfileUseCase {

    private final IServiceProviderRepository repository;
    private final IUserContext userContext;

    @Transactional
    public ServiceProvider execute(Input input) {
        // 1. Segurança: Busca o ProviderId do contexto do usuário logado
        // Isso impede que um dono de salão tente editar o perfil de outro enviando o ID no body.
        UUID providerId = userContext.getCurrentUser().getProviderId();

        ServiceProvider provider = repository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 2. Validação de Unicidade de Slug (URL customizada)
        if (input.slug() != null && !input.slug().equals(provider.getPublicProfileSlug().value())) {
            Slug newSlug = new Slug(input.slug());
            if (repository.existsBySlug(newSlug.value())) {
                throw new BusinessException("Esta URL amigável já está em uso por outro estabelecimento.");
            }
            provider.updateSlug(newSlug);
        }

        // 3. Atualização de Dados Básicos e Endereço
        provider.updateProfile(
            input.name(), 
            input.phoneNumber(), 
            input.logoUrl()
        );

        if (input.address() != null) {
            provider.updateAddress(input.address()); 
        }

        return repository.save(provider);
    }

    public record Input(
            String name,
            String phoneNumber,
            String logoUrl,
            String slug,
            com.stylo.api_agendamento.core.domain.vo.Address address
    ) {}
}