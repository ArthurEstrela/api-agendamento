package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.AddressRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.RegisterServiceProviderRequest;
import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Service; // ✨ IMPORT ADICIONADO
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Address;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IServiceRepository; // ✨ IMPORT ADICIONADO
import com.stylo.api_agendamento.core.usecases.ListProfessionalsByProviderUseCase; // ✨ IMPORT ADICIONADO
import com.stylo.api_agendamento.core.usecases.RegisterServiceProviderUseCase;
import com.stylo.api_agendamento.core.usecases.SearchServiceProvidersUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateServiceProviderProfileUseCase;
import com.stylo.api_agendamento.core.usecases.dto.ProfessionalProfile; // ✨ IMPORT ADICIONADO
import com.stylo.api_agendamento.core.usecases.dto.ProviderSearchCriteria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List; // ✨ IMPORT ADICIONADO
import java.util.UUID; // ✨ IMPORT ADICIONADO

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/service-providers")
@RequiredArgsConstructor
@Tag(name = "Estabelecimentos (Tenant)", description = "Onboarding e gestão de perfil de salões, clínicas e barbearias")
public class ServiceProviderController {

        private final RegisterServiceProviderUseCase registerUseCase;
        private final UpdateServiceProviderProfileUseCase updateProfileUseCase;
        private final SearchServiceProvidersUseCase searchServiceProvidersUseCase;
        
        // ✨ Repositório injetado para resolver a busca pública
        private final IServiceProviderRepository repository; 

        // ✨ INJEÇÕES ADICIONADAS PARA O ENDPOINT AGREGADOR (BFF)
        private final IServiceRepository serviceRepository;
        private final ListProfessionalsByProviderUseCase listProfessionalsUseCase;

        @Operation(summary = "Criar Conta (Onboarding)", description = "Regista um novo estabelecimento e sincroniza com o Firebase.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Estabelecimento registado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já em uso")
        })
        @PostMapping("/register")
        public ResponseEntity<ServiceProvider> register(@RequestBody @Valid RegisterServiceProviderRequest request) {

                // 1. Geração do Slug baseada no nome do negócio
                String normalized = Normalizer.normalize(request.businessName().trim(), Normalizer.Form.NFD)
                                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

                String generatedSlugValue = normalized.toLowerCase()
                                .replaceAll("[^a-z0-9\\s]", "")
                                .replaceAll("\\s+", "-");

                // 2. Extrair o endereço que AGORA VEM do Frontend na primeira requisição
                Address addressDomain = null;
                if (request.address() != null) {
                        addressDomain = request.address().toDomain();
                } else {
                        throw new IllegalArgumentException("O endereço do estabelecimento é obrigatório.");
                }

                // 3. Tratamento do Documento (CPF/CNPJ)
                String cleanDoc = request.document().replaceAll("\\D", "");
                Document.DocumentType type = cleanDoc.length() > 11 ? Document.DocumentType.CNPJ
                                : Document.DocumentType.CPF;
                Document document = new Document(cleanDoc, type);

                // 4. Instância do Record 'Input' mapeando os nomes
                var input = new RegisterServiceProviderUseCase.Input(
                                request.businessName(),
                                document,
                                new Slug(generatedSlugValue),
                                addressDomain,
                                request.ownerName(),
                                request.ownerEmail(),
                                request.ownerPassword(),
                                request.phone(),
                                true, // Por padrão, o dono é o primeiro profissional da agenda
                                request.firebaseUid());

                return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.execute(input));
        }

        // ✨ ENDPOINT DE ALTA PERFORMANCE (BFF) PARA O AGENDAMENTO PÚBLICO ✨
        @Operation(summary = "Buscar Dados Completos para Agendamento", description = "Retorna o provedor, equipe e serviços ativos em uma única requisição de alta performance.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Dados encontrados com sucesso"),
                @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado")
        })
        @GetMapping("/public/{id}/booking-data")
        public ResponseEntity<PublicBookingDataResponse> getPublicBookingData(@PathVariable UUID id) {
            
            // 1. Busca os dados da Barbearia/Salão
            ServiceProvider provider = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado para o ID: " + id));

            // 2. Busca a equipe
            List<ProfessionalProfile> professionals = listProfessionalsUseCase.execute(id);
            
            // 3. Busca apenas os serviços ativos
            List<Service> services = serviceRepository.findAllActiveByProviderId(id);

            // Retorna tudo consolidado num único JSON
            return ResponseEntity.ok(new PublicBookingDataResponse(provider, professionals, services));
        }

        @Operation(summary = "Buscar Perfil Público por Slug", description = "Retorna os dados públicos de um estabelecimento usando a sua URL amigável (ex: /public/slug/arthurbarber).")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Perfil encontrado com sucesso"),
                @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado para o slug informado")
        })
        @GetMapping("/public/slug/{slugValue}")
        public ResponseEntity<ServiceProvider> getPublicProfileBySlug(@PathVariable String slugValue) {
            
            Slug slug = new Slug(slugValue);
            
            ServiceProvider provider = repository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado para o slug: " + slugValue));
                
            return ResponseEntity.ok(provider);
        }

        @GetMapping("/search")
        @Operation(summary = "Busca avançada de estabelecimentos", description = "Retorna estabelecimentos aplicando filtros dinâmicos de nome, cidade, preço e avaliação.")
        public ResponseEntity<PagedResult<ServiceProvider>> search(
                        @RequestParam(required = false) String searchTerm,
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) Double minRating,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                ProviderSearchCriteria criteria = new ProviderSearchCriteria(
                                searchTerm, city, minRating, minPrice, maxPrice);

                PagedResult<ServiceProvider> result = searchServiceProvidersUseCase.execute(criteria, page, size);

                return ResponseEntity.ok(result);
        }

        @Operation(summary = "Atualizar Perfil do Estabelecimento", description = "Atualiza os dados públicos (logo, banner, url/slug, morada) do salão.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
                        @ApiResponse(responseCode = "400", description = "O Slug escolhido já está em uso"),
                        @ApiResponse(responseCode = "403", description = "Acesso negado: Apenas o dono pode alterar isto")
        })
        @PutMapping("/profile")
        @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
        public ResponseEntity<ServiceProvider> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {

                Address addressDomain = request.address() != null ? request.address().toDomain() : null;

                var input = new UpdateServiceProviderProfileUseCase.Input(
                                request.name(),
                                request.phoneNumber(),
                                request.logoUrl(),
                                request.bannerUrl(),
                                request.slug(),
                                addressDomain);

                return ResponseEntity.ok(updateProfileUseCase.execute(input));
        }

        public record UpdateProfileRequest(
                        String name,
                        String phoneNumber,
                        String logoUrl,
                        String bannerUrl,
                        String slug,
                        AddressRequest address) {
        }

        // ✨ RECORD DE RESPOSTA DO NOVO ENDPOINT AGREGADOR ✨
        public record PublicBookingDataResponse(
                        ServiceProvider provider,
                        List<ProfessionalProfile> professionals,
                        List<Service> services
        ) {}
}