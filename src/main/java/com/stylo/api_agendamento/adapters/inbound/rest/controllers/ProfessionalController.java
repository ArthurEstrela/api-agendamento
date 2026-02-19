package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.*;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.usecases.BlockProfessionalTimeUseCase;
import com.stylo.api_agendamento.core.usecases.GetProfessionalProfileUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateProfessionalAvailabilityUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateProfessionalCommissionUseCase;
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

    @Operation(summary = "Obter perfil do profissional", description = "Retorna os dados públicos do profissional (nome, serviços, agenda disponível).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso")
    })
    @GetMapping("/{id}/profile")
    @PreAuthorize("isAuthenticated()") // Qualquer utilizador logado (clientes inclusive) pode ver o perfil
    public ResponseEntity<ProfessionalProfile> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(getProfessionalProfileUseCase.execute(id));
    }

    @Operation(summary = "Atualizar Grade de Horários", description = "Define os dias e horários de trabalho recorrentes do profissional.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Horários atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Conflito de horários fornecidos")
    })
    @PutMapping("/{id}/availability")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> updateAvailability(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateAvailabilityRequest request) {

        // Mapeamento do DTO para o VO de domínio
        List<DailyAvailability> availabilities = request.availabilities().stream()
                .map(req -> new DailyAvailability(
                        req.dayOfWeek(),
                        req.isWorkingDay(), // Se no DTO estiver isOpen(), altere aqui
                        req.startTime(),
                        req.endTime()))
                .collect(Collectors.toList());

        var input = new UpdateProfessionalAvailabilityUseCase.Input(id, availabilities);

        updateAvailabilityUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Bloquear Horário na Agenda", description = "Cria uma exceção pontual na agenda (ex: Almoço prolongado, Consulta médica).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Horário bloqueado com sucesso"),
            @ApiResponse(responseCode = "409", description = "Horário já ocupado por agendamentos")
    })
    @PostMapping("/{id}/block")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> blockTime(
            @PathVariable UUID id,
            @RequestBody @Valid BlockProfessionalTimeRequest request) {

        var input = new BlockProfessionalTimeUseCase.Input(
                id,
                request.startTime(),
                request.endTime(),
                request.reason()
        );

        blockTimeUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Atualizar Comissão (Staff)", description = "Altera as regras e percentagens de comissão de um profissional específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comissão atualizada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: Apenas o dono do estabelecimento pode alterar comissões")
    })
    @PatchMapping("/{id}/commission")
    // Segurança estrita: Apenas o gestor financeiro ou dono do estabelecimento
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Void> updateCommission(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateCommissionRequest request) {

        // ✨ CORREÇÃO AQUI: Passamos apenas os 3 parâmetros que o Record Input pede.
        // O ID do dono (requesterId) já é capturado automaticamente por dentro do UseCase!
        var input = new UpdateProfessionalCommissionUseCase.Input(
                id,             // O ID do profissional alvo
                request.type(),
                request.value()
        );

        updateCommissionUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }
}