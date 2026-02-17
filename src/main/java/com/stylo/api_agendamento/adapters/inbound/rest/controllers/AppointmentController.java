package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment.*;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.usecases.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Gerenciamento completo de agendamentos (Online, Manual, Recorrente e Bloqueios)")
public class AppointmentController {

    private final CreateAppointmentUseCase createAppointmentUseCase;
    private final CreateManualAppointmentUseCase createManualAppointmentUseCase;
    private final CreateRecurringAppointmentUseCase createRecurringAppointmentUseCase; // ✨ Novo UseCase
    private final ConfirmAppointmentUseCase confirmAppointmentUseCase;
    private final CompleteAppointmentUseCase completeAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final RescheduleAppointmentUseCase rescheduleAppointmentUseCase;
    private final GetAvailableSlotsUseCase getAvailableSlotsUseCase;
    private final GetProfessionalAvailabilityUseCase getProfessionalAvailabilityUseCase;
    private final MarkNoShowUseCase markNoShowUseCase;
    
    private final IUserContext userContext;

    // --- AGENDAMENTO ONLINE ---

    @Operation(summary = "Criar Agendamento Online", description = "Endpoint para o cliente final agendar um horário via App/Site.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou regras de negócio violadas"),
        @ApiResponse(responseCode = "409", description = "Conflito de horário (Horário já ocupado)")
    })
    @PostMapping
    public ResponseEntity<Appointment> create(@RequestBody @Valid CreateAppointmentRequest request) {
        String loggedUserId = userContext.getCurrentUserId();
        
        var input = new CreateAppointmentUseCase.CreateAppointmentInput(
                loggedUserId, // ID do cliente logado
                request.professionalId(),
                request.serviceIds(),
                request.startTime(),
                request.reminderMinutes());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createAppointmentUseCase.execute(input));
    }

    // --- AGENDAMENTO RECORRENTE (NOVO) ---

    @Operation(summary = "Criar Agendamento Recorrente", description = "Cria uma série de agendamentos (ex: Toda Sexta-feira). Retorna relatório de sucessos e falhas.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Processo concluído (pode haver falhas parciais)"),
        @ApiResponse(responseCode = "409", description = "Nenhum agendamento pôde ser criado (todos falharam)")
    })
    @PostMapping("/recurring")
    public ResponseEntity<RecurringAppointmentResponse> createRecurring(@RequestBody @Valid RecurringAppointmentRequest request) {
        String loggedUserId = userContext.getCurrentUserId();

        var input = CreateRecurringAppointmentUseCase.RecurringInput.builder()
                .clientId(loggedUserId)
                .professionalId(request.professionalId())
                .serviceIds(request.serviceIds())
                .firstStartTime(request.firstStartTime())
                .recurrenceType(request.recurrenceType())
                .occurrences(request.occurrences())
                .endDate(request.endDate())
                .reminderMinutes(request.reminderMinutes())
                .build();

        var result = createRecurringAppointmentUseCase.execute(input);

        // Mapeia resposta do Domínio para DTO
        var response = new RecurringAppointmentResponse(
                result.getSummaryMessage(),
                result.createdAppointments().size(),
                result.failedAppointments().size(),
                result.createdAppointments().stream().map(Appointment::getId).toList(),
                result.failedAppointments().stream()
                        .map(f -> new RecurringAppointmentResponse.FailedSlot(f.date(), f.reason()))
                        .toList()
        );

        if (result.createdAppointments().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- AGENDAMENTO MANUAL (WALK-IN) ---

    @Operation(summary = "Criar Agendamento Manual", description = "Uso exclusivo do Profissional/Recepção para agendar clientes sem app.")
    @PostMapping("/manual")
    public ResponseEntity<Appointment> createManual(@RequestBody @Valid CreateManualAppointmentRequest request) {
        var input = new CreateManualAppointmentUseCase.ManualInput(
                request.professionalId(),
                request.clientName(),
                request.clientPhone(),
                request.serviceIds(),
                request.startTime(),
                request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(createManualAppointmentUseCase.execute(input));
    }

    // --- FLUXO DE VIDA DO AGENDAMENTO ---

    @Operation(summary = "Confirmar Agendamento", description = "Profissional confirma um agendamento pendente.")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String id) {
        String loggedUserId = userContext.getCurrentUserId();
        var input = new ConfirmAppointmentUseCase.ConfirmAppointmentInput(id, loggedUserId);
        confirmAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Finalizar/Checkout", description = "Conclui o atendimento, calcula comissões e registra pagamento.")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable String id,
            @RequestBody @Valid CompleteAppointmentRequest request) {

        var productItems = request.soldProducts() != null
                ? request.soldProducts().stream()
                .map(p -> new CompleteAppointmentUseCase.ProductSaleItem(p.productId(), p.quantity()))
                .toList()
                : List.<CompleteAppointmentUseCase.ProductSaleItem>of();

        var input = new CompleteAppointmentUseCase.CompleteAppointmentInput(
                id,
                PaymentMethod.valueOf(request.paymentMethod()),
                request.serviceFinalPrice(),
                productItems
        );

        completeAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancelar Agendamento", description = "Cancela o agendamento. Se for cliente, aplica regras de antecedência.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Cancelado via sistema") String reason) {

        String loggedUserId = userContext.getCurrentUserId();
        UserRole role = userContext.getCurrentUserRole();
        boolean isClient = (role == UserRole.CLIENT);

        var input = new CancelAppointmentUseCase.CancelAppointmentInput(
                id,
                loggedUserId,
                reason,
                isClient);

        cancelAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reagendar", description = "Move um agendamento para um novo horário, validando conflitos.")
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> reschedule(
            @PathVariable String id,
            @RequestBody @Valid RescheduleAppointmentRequest request) {
        
        var input = new RescheduleAppointmentUseCase.RescheduleInput(id, request.newStartTime());
        return ResponseEntity.ok(rescheduleAppointmentUseCase.execute(input));
    }

    @Operation(summary = "Marcar No-Show", description = "Indica que o cliente não compareceu.")
    @PatchMapping("/{id}/no-show")
    public ResponseEntity<Void> markNoShow(@PathVariable String id) {
        markNoShowUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // --- DISPONIBILIDADE ---

    @Operation(summary = "Buscar Horários Livres (Simples)", description = "Retorna lista de horários disponíveis para um dia específico.")
    @GetMapping("/slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @Parameter(description = "ID do profissional") @RequestParam String professionalId,
            @Parameter(description = "Data no formato YYYY-MM-DD") @RequestParam String date) {
        
        var input = new GetAvailableSlotsUseCase.AvailableSlotsInput(
                professionalId,
                LocalDate.parse(date),
                List.of()
        );
        return ResponseEntity.ok(getAvailableSlotsUseCase.execute(input));
    }

    @Operation(summary = "Verificar Disponibilidade Completa", description = "Verifica se uma lista de serviços cabe na agenda em determinada data.")
    @GetMapping("/availability")
    public ResponseEntity<List<LocalTime>> getAvailability(
            @RequestParam String professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam List<String> serviceIds) {
        
        var availability = getProfessionalAvailabilityUseCase.execute(
                professionalId,
                date,
                serviceIds);
        return ResponseEntity.ok(availability);
    }
}