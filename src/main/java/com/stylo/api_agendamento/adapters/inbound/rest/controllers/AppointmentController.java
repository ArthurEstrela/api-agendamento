package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment.*;
import com.stylo.api_agendamento.adapters.inbound.rest.idempotency.Idempotent;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.usecases.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Gerenciamento completo de agendamentos (Online, Manual, Recorrente e Bloqueios)")
public class AppointmentController {

    private final CreateAppointmentUseCase createAppointmentUseCase;
    private final CreateManualAppointmentUseCase createManualAppointmentUseCase;
    private final CreateRecurringAppointmentUseCase createRecurringAppointmentUseCase;
    private final ConfirmAppointmentUseCase confirmAppointmentUseCase;
    private final CompleteAppointmentUseCase completeAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final RescheduleAppointmentUseCase rescheduleAppointmentUseCase;
    private final GetAvailableSlotsUseCase getAvailableSlotsUseCase;
    private final GetProfessionalAvailabilityUseCase getProfessionalAvailabilityUseCase;
    private final MarkNoShowUseCase markNoShowUseCase;
    private final AddAppointmentItemUseCase addAppointmentItemUseCase;
    private final RemoveAppointmentItemUseCase removeAppointmentItemUseCase;

    private final IUserContext userContext;

    // --- AGENDAMENTO ONLINE (Clientes e Staff) ---

    @Operation(summary = "Criar Agendamento Online", description = "Endpoint para o cliente final agendar um horário via App/Site.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regras de negócio violadas"),
            @ApiResponse(responseCode = "409", description = "Conflito de horário (Horário já ocupado)")
    })
    @PostMapping
    @Idempotent(ttl = 24, unit = TimeUnit.HOURS)
    @PreAuthorize("hasAuthority('appointment:write')")
    public ResponseEntity<Appointment> create(@RequestBody @Valid CreateAppointmentRequest request) {
        UUID loggedUserId = userContext.getCurrentUserId();

        var input = new CreateAppointmentUseCase.Input(
                loggedUserId,
                UUID.fromString(request.professionalId()),
                request.serviceIds().stream().map(UUID::fromString).collect(Collectors.toList()),
                request.startTime(),
                request.reminderMinutes(),
                request.couponCode(),
                null // notes
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(createAppointmentUseCase.execute(input));
    }

    @Operation(summary = "Criar Agendamento Recorrente", description = "Cria uma série de agendamentos (ex: Toda Sexta-feira).")
    @PostMapping("/recurring")
    @PreAuthorize("hasAuthority('appointment:write')")
    public ResponseEntity<RecurringAppointmentResponse> createRecurring(@RequestBody @Valid RecurringAppointmentRequest request) {
        UUID loggedUserId = userContext.getCurrentUserId();

        var input = CreateRecurringAppointmentUseCase.Input.builder()
                .clientId(loggedUserId)
                .professionalId(UUID.fromString(request.professionalId()))
                .serviceIds(request.serviceIds().stream().map(UUID::fromString).collect(Collectors.toList()))
                .firstStartTime(request.firstStartTime())
                .recurrenceType(request.recurrenceType())
                .occurrences(request.occurrences())
                .endDate(request.endDate())
                .reminderMinutes(request.reminderMinutes())
                .build();

        CreateRecurringAppointmentUseCase.Response result = createRecurringAppointmentUseCase.execute(input);

        // ✅ CORREÇÃO AQUI: Mapeando a extração do ID (UUID) para String
        var response = new RecurringAppointmentResponse(
                result.getSummary(),
                result.createdAppointments().size(),
                result.failedOccurrences().size(),
                result.createdAppointments().stream().map(a -> a.getId().toString()).toList(),
                result.failedOccurrences().stream()
                        .map(f -> new RecurringAppointmentResponse.FailedSlot(f.date(), f.reason()))
                        .toList()
        );

        if (result.createdAppointments().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- AGENDAMENTO MANUAL (Apenas Staff) ---

    @Operation(summary = "Criar Agendamento Manual", description = "Uso exclusivo do Profissional/Recepção para agendar clientes sem app.")
    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Appointment> createManual(@RequestBody @Valid CreateManualAppointmentRequest request) {
        var input = new CreateManualAppointmentUseCase.Input(
                UUID.fromString(request.professionalId()),
                request.clientName(),
                request.clientPhone(),
                request.serviceIds().stream().map(UUID::fromString).collect(Collectors.toList()),
                request.startTime(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createManualAppointmentUseCase.execute(input));
    }

    // --- FLUXO DE VIDA DO AGENDAMENTO ---

    @Operation(summary = "Confirmar Agendamento", description = "Profissional ou Recepção confirma um agendamento pendente.")
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> confirm(@PathVariable UUID id) {
        UUID providerId = userContext.getCurrentUser().getProviderId();
        var input = new ConfirmAppointmentUseCase.Input(id, providerId);
        confirmAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Finalizar/Checkout", description = "Conclui o atendimento, calcula comissões e registra pagamento.")
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> complete(@PathVariable UUID id, @RequestBody @Valid CompleteAppointmentRequest request) {
        var productItems = request.soldProducts() != null
                ? request.soldProducts().stream()
                .map(p -> new CompleteAppointmentUseCase.ProductSaleItem(UUID.fromString(p.productId()), p.quantity()))
                .collect(Collectors.toList())
                : Collections.<CompleteAppointmentUseCase.ProductSaleItem>emptyList();

        var input = new CompleteAppointmentUseCase.Input(
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
    @PreAuthorize("hasAuthority('appointment:write')")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Cancelado via sistema") String reason) {

        UUID loggedUserId = userContext.getCurrentUserId();
        UserRole role = userContext.getCurrentUserRole();
        boolean isClient = (role == UserRole.CLIENT);

        var input = new CancelAppointmentUseCase.Input(
                id,
                loggedUserId,
                reason,
                isClient
        );

        cancelAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reagendar", description = "Move um agendamento para um novo horário, validando conflitos.")
    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('appointment:write')")
    public ResponseEntity<Appointment> reschedule(
            @PathVariable UUID id,
            @RequestBody @Valid RescheduleAppointmentRequest request) {

        var input = new RescheduleAppointmentUseCase.Input(id, request.newStartTime());
        return ResponseEntity.ok(rescheduleAppointmentUseCase.execute(input));
    }

    @Operation(summary = "Marcar No-Show", description = "Indica que o cliente não compareceu (Uso exclusivo do Staff).")
    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> markNoShow(@PathVariable UUID id) {
        markNoShowUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // --- DISPONIBILIDADE (Leitura Pública / Autenticada) ---

    @Operation(summary = "Buscar Horários Livres (Simples)", description = "Retorna lista de horários disponíveis para um dia específico.")
    @GetMapping("/slots")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @Parameter(description = "ID do profissional") @RequestParam UUID professionalId,
            @Parameter(description = "Data no formato YYYY-MM-DD") @RequestParam String date) {

        var input = new GetAvailableSlotsUseCase.Input(
                professionalId,
                LocalDate.parse(date),
                Collections.emptyList()
        );
        return ResponseEntity.ok(getAvailableSlotsUseCase.execute(input));
    }

    @Operation(summary = "Verificar Disponibilidade Completa", description = "Verifica se uma lista de serviços cabe na agenda em determinada data.")
    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<LocalTime>> getAvailability(
            @RequestParam UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam List<UUID> serviceIds) {

        var availability = getProfessionalAvailabilityUseCase.execute(professionalId, date, serviceIds);
        return ResponseEntity.ok(availability);
    }

    // --- MANEJO DE COMANDA (Apenas Staff) ---

    @Operation(summary = "Adicionar Produto à Comanda", description = "Adiciona um item ao agendamento e deduz do estoque imediatamente.")
    @PostMapping("/{id}/items")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Appointment> addItem(@PathVariable UUID id, @RequestBody @Valid AddItemRequest request) {
        var input = new AddAppointmentItemUseCase.Input(id, UUID.fromString(request.productId()), request.quantity());
        return ResponseEntity.ok(addAppointmentItemUseCase.execute(input));
    }

    @Operation(summary = "Remover Produto da Comanda", description = "Remove um item do agendamento e devolve ao estoque.")
    @DeleteMapping("/{id}/items/{productId}")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Appointment> removeItem(@PathVariable UUID id, @PathVariable UUID productId) {
        return ResponseEntity.ok(removeAppointmentItemUseCase.execute(id, productId));
    }
}