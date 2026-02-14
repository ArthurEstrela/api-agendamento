package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment.*;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.ports.IUserContext; // ✨ Import novo
import com.stylo.api_agendamento.core.usecases.*;
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
public class AppointmentController {

    private final CreateAppointmentUseCase createAppointmentUseCase;
    private final CreateManualAppointmentUseCase createManualAppointmentUseCase;
    private final ConfirmAppointmentUseCase confirmAppointmentUseCase;
    private final CompleteAppointmentUseCase completeAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final RescheduleAppointmentUseCase rescheduleAppointmentUseCase;
    private final GetAvailableSlotsUseCase getAvailableSlotsUseCase;
    private final GetProfessionalAvailabilityUseCase getProfessionalAvailabilityUseCase;
    private final MarkNoShowUseCase markNoShowUseCase;
    
    // ✨ Injeção do Contexto de Usuário
    private final IUserContext userContext; 

    @PostMapping
    public ResponseEntity<Appointment> create(@RequestBody @Valid CreateAppointmentRequest request) {
        // ✨ Segurança: Forçamos o ID do cliente logado, ignorando o que vier no JSON se for diferente.
        // Ou validamos se o usuário logado é o mesmo do request.
        String loggedUserId = userContext.getCurrentUserId();
        
        // Se for um CLIENT criando, usamos o ID dele. 
        // Se for ADMIN/PROFESSIONAL criando para outro, poderíamos permitir o request.clientId().
        // Assumindo fluxo padrão de app mobile (Cliente agendando):
        String clientIdToUse = loggedUserId; 

        var input = new CreateAppointmentUseCase.CreateAppointmentInput(
                clientIdToUse,
                request.professionalId(),
                request.serviceIds(),
                request.startTime(),
                request.reminderMinutes());
        return ResponseEntity.status(HttpStatus.CREATED).body(createAppointmentUseCase.execute(input));
    }

    @PostMapping("/manual")
    public ResponseEntity<Appointment> createManual(@RequestBody @Valid CreateManualAppointmentRequest request) {
        // Apenas profissionais podem criar manual, validação já feita pelo SecurityConfig (Role)
        var input = new CreateManualAppointmentUseCase.ManualInput(
                request.professionalId(),
                request.clientName(),
                request.clientPhone(),
                request.serviceIds(),
                request.startTime(),
                request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(createManualAppointmentUseCase.execute(input));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String id) {
        // ✨ Obtendo o ID real do usuário logado (que é o provider/profissional)
        String loggedUserId = userContext.getCurrentUserId();

        var input = new ConfirmAppointmentUseCase.ConfirmAppointmentInput(id, loggedUserId);
        confirmAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable String id,
            @RequestBody @Valid CompleteAppointmentRequest request) {

        var productItems = request.soldProducts() != null
                ? request.soldProducts().stream()
                .map(p -> new CompleteAppointmentUseCase.ProductSaleItem(p.productId(),
                        p.quantity()))
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Cancelado via sistema") String reason) {

        // ✨ Recuperação segura do usuário
        String loggedUserId = userContext.getCurrentUserId();
        UserRole role = userContext.getCurrentUserRole();
        
        // ✨ Lógica inteligente: Se for CLIENT, é cancelamento pelo cliente. Se não, é pelo estabelecimento.
        boolean isClient = (role == UserRole.CLIENT);

        var input = new CancelAppointmentUseCase.CancelAppointmentInput(
                id,
                loggedUserId,
                reason,
                isClient);

        cancelAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }
    
    // ... (restante dos métodos mantém-se igual, pois são GET públicos ou não dependem de user ID crítico)
    
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> reschedule(
                    @PathVariable String id,
                    @RequestBody @Valid RescheduleAppointmentRequest request) {
            // Nota: Adicione validação aqui se quiser garantir que só o dono do agendamento pode reagendar
            var input = new RescheduleAppointmentUseCase.RescheduleInput(
                            id,
                            request.newStartTime());
            return ResponseEntity.ok(rescheduleAppointmentUseCase.execute(input));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
                    @RequestParam String professionalId,
                    @RequestParam String date) {
            var input = new GetAvailableSlotsUseCase.AvailableSlotsInput(
                            professionalId,
                            LocalDate.parse(date),
                            List.of() 
            );
            return ResponseEntity.ok(getAvailableSlotsUseCase.execute(input));
    }

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

    @PatchMapping("/{id}/no-show")
    public ResponseEntity<Void> markNoShow(@PathVariable String id) {
            markNoShowUseCase.execute(id);
            return ResponseEntity.noContent().build();
    }
}