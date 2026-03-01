package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.*;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.usecases.*;
import com.stylo.api_agendamento.core.usecases.dto.ProfessionalProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/professionals")
@RequiredArgsConstructor
@Tag(name = "Profissionais", description = "Gestão de perfis, escalas de trabalho, bloqueios de horário e comissões")
public class ProfessionalController {

        private final GetProfessionalProfileUseCase getProfessionalProfileUseCase;
        private final UpdateProfessionalAvailabilityUseCase updateAvailabilityUseCase;
        private final BlockProfessionalTimeUseCase blockTimeUseCase;
        private final UpdateProfessionalCommissionUseCase updateCommissionUseCase;
        private final CreateProfessionalUseCase createProfessionalUseCase;
        private final UpdateProfessionalServicesUseCase updateProfessionalServicesUseCase;
        private final UpdateProfessionalProfileUseCase updateProfessionalProfileUseCase;
        private final DeleteProfessionalUseCase deleteProfessionalUseCase;

        // ✨ INJETANDO O NOVO USE CASE
        private final ListProfessionalsByProviderUseCase listProfessionalsByProviderUseCase;

        @Operation(summary = "Criar Profissional (Staff)", description = "Cria um novo profissional vinculado a um estabelecimento.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Profissional criado com sucesso")
        })
        @PostMapping
        @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
        public ResponseEntity<ProfessionalProfile> createProfessional(
                        @RequestBody @Valid CreateProfessionalRequest request) {

                var input = new CreateProfessionalUseCase.Input(
                                request.providerId(),
                                request.name(),
                                request.email(),
                                request.bio(),
                                request.commissionPercentage(),
                                request.serviceIds(),
                                request.isOwner());

                var created = createProfessionalUseCase.execute(input);
                return ResponseEntity.status(201).body(getProfessionalProfileUseCase.execute(created.getId()));
        }

        @Operation(summary = "Obter perfil do profissional", description = "Retorna os dados públicos do profissional (nome, serviços, agenda disponível).")
        @GetMapping("/{id}/profile")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProfessionalProfile> getProfile(@PathVariable UUID id) {
                return ResponseEntity.ok(getProfessionalProfileUseCase.execute(id));
        }

        @Operation(summary = "Atualizar Grade de Horários", description = "Define os dias e horários de trabalho recorrentes do profissional.")
        @PutMapping("/{id}/availability")
        @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
        public ResponseEntity<Void> updateAvailability(
                        @PathVariable UUID id,
                        @RequestBody @Valid UpdateAvailabilityRequest request) {

                List<DailyAvailability> availabilities = request.availabilities().stream()
                                .map(req -> new DailyAvailability(
                                                req.dayOfWeek(),
                                                req.isWorkingDay(),
                                                req.startTime(),
                                                req.endTime()))
                                .collect(Collectors.toList());

                var input = new UpdateProfessionalAvailabilityUseCase.Input(id, availabilities);
                updateAvailabilityUseCase.execute(input);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Bloquear Horário na Agenda", description = "Cria uma exceção pontual na agenda.")
        @PostMapping("/{id}/block")
        @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
        public ResponseEntity<Void> blockTime(
                        @PathVariable UUID id,
                        @RequestBody @Valid BlockProfessionalTimeRequest request) {

                var input = new BlockProfessionalTimeUseCase.Input(
                                id,
                                request.startTime(),
                                request.endTime(),
                                request.reason());

                blockTimeUseCase.execute(input);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Atualizar Comissão (Staff)", description = "Altera as regras de comissão.")
        @PatchMapping("/{id}/commission")
        @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
        public ResponseEntity<Void> updateCommission(
                        @PathVariable UUID id,
                        @RequestBody @Valid UpdateCommissionRequest request) {

                var input = new UpdateProfessionalCommissionUseCase.Input(
                                id,
                                request.type(),
                                request.value());

                updateCommissionUseCase.execute(input);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Vincular Serviços", description = "Atualiza a lista de serviços que o profissional realiza.")
        @PutMapping("/{id}/services")
        @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
        public ResponseEntity<Void> updateServices(
                        @PathVariable UUID id,
                        @RequestBody @Valid UpdateProfessionalServicesRequest request) {

                updateProfessionalServicesUseCase.execute(id, request.serviceIds());
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Atualizar Perfil Básico", description = "Atualiza nome e bio do profissional.")
        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
        public ResponseEntity<ProfessionalProfile> updateBasicProfile(
                        @PathVariable UUID id,
                        @RequestBody @Valid UpdateProfessionalBasicRequest request) {

                var input = new UpdateProfessionalProfileUseCase.Input(request.name(), request.bio());
                var updatedProfessional = updateProfessionalProfileUseCase.execute(id, input);

                return ResponseEntity.ok(getProfessionalProfileUseCase.execute(updatedProfessional.getId()));
        }

        @Operation(summary = "Inativar Profissional (Soft Delete)")
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
        public ResponseEntity<Void> deleteProfessional(@PathVariable UUID id) {
                deleteProfessionalUseCase.execute(id);
                return ResponseEntity.noContent().build();
        }

        // ✨ O CÓDIGO CORRIGIDO QUE FALTAVA
        @Operation(summary = "Listar Profissionais do Estabelecimento")
        @GetMapping("/provider/{providerId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<List<ProfessionalProfile>> listProfessionalsByProvider(@PathVariable UUID providerId) {
                // Chama o UseCase e retorna a lista em JSON com status 200 OK
                List<ProfessionalProfile> profiles = listProfessionalsByProviderUseCase.execute(providerId);
                return ResponseEntity.ok(profiles);
        }
}