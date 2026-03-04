package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.domain.vo.SocialLinks; // ✨ IMPORT ADICIONADO
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
        UUID providerId = userContext.getCurrentUser().getProviderId();

        ServiceProvider provider = repository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 2. Validação Segura de Unicidade de Slug (Evita NullPointerException)
        String currentSlug = provider.getPublicProfileSlug() != null ? provider.getPublicProfileSlug().value() : null;
        
        if (input.slug() != null && !input.slug().equals(currentSlug)) {
            Slug newSlug = new Slug(input.slug());
            if (repository.existsBySlug(newSlug.value())) {
                throw new BusinessException("Esta URL amigável já está em uso por outro estabelecimento.");
            }
            provider.updateSlug(newSlug);
        }

        // 3. Mapeamento das Redes Sociais para o Domínio
        SocialLinks linksDomain = null;
        if (input.socialLinks() != null) {
            linksDomain = new SocialLinks(
                input.socialLinks().instagram(),
                input.socialLinks().facebook(),
                input.socialLinks().website(),
                input.socialLinks().whatsapp()
            );
        }

        // 4. Atualização de Dados Básicos (Métodos In-Place do Domínio)
        provider.updateProfile(
            input.businessName(), // Usando o businessName no lugar de name genérico
            input.phoneNumber(), 
            input.logoUrl(),
            input.bannerUrl(),
            linksDomain           // ✨ PASSANDO AS REDES SOCIAIS AQUI
        );

        // 5. Atualização de Endereço
        if (input.address() != null) {
            provider.updateAddress(input.address()); 
        }

        // 6. Atualização de Dados de Pagamento (Pix)
        if (input.pixKey() != null || input.pixKeyType() != null) {
            provider.updatePixKeys(input.pixKey(), input.pixKeyType());
        }

        // 7. Atualização de Configurações via Builder (Para campos que não possuem setters no Domínio)
        ServiceProvider updatedProvider = provider.toBuilder()
                .cancellationMinHours(input.cancellationMinHours() != null ? input.cancellationMinHours() : provider.getCancellationMinHours())
                .build();

        return repository.save(updatedProvider);
    }

    // ✨ Input completo mapeando a requisição do Controller
    public record Input(
            String name, 
            String businessName,
            String phoneNumber,
            String logoUrl,
            String bannerUrl,
            String slug,
            com.stylo.api_agendamento.core.domain.vo.Address address,
            String documentType,
            String document,
            Integer cancellationMinHours,
            String pixKey,
            String pixKeyType,
            SocialLinksInput socialLinks // ✨ Campo das redes sociais adicionado
    ) {}

    // ✨ Sub-record para transitar as redes sociais sem sujar o UseCase com o DTO do Rest
    public record SocialLinksInput(
        String instagram,
        String facebook,
        String website,
        String whatsapp
    ) {}
}