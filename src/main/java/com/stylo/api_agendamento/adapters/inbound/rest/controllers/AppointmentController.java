package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment.*;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.usecases.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    public ResponseEntity<Appointment> create(@RequestBody @Valid CreateAppointmentRequest request) {
        var input = new CreateAppointmentUseCase.CreateAppointmentInput(
                request.clientId(),
                request.professionalId(),
                request.serviceIds(),
                request.startTime(),
                request.reminderMinutes());
        return ResponseEntity.status(HttpStatus.CREATED).body(createAppointmentUseCase.execute(input));
    }

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

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String id) {
        confirmAppointmentUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable String id,
            @RequestBody @Valid CompleteAppointmentRequest request) {

        // 1. Converter os itens do Request (DTO) para os itens do UseCase (Input)
        var productItems = request.soldProducts() != null
                ? request.soldProducts().stream()
                        .map(p -> new CompleteAppointmentUseCase.ProductSaleItem(p.productId(), p.quantity()))
                        .toList()
                : List.<CompleteAppointmentUseCase.ProductSaleItem>of();

        // 2. Criar o input com os 4 argumentos corretos
        var input = new CompleteAppointmentUseCase.CompleteAppointmentInput(
                id,
                PaymentMethod.valueOf(request.paymentMethod()),
                request.serviceFinalPrice(), // <--- Correção: Usa serviceFinalPrice()
                productItems // <--- Correção: Passa a lista de produtos
        );

        completeAppointmentUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        cancelAppointmentUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> reschedule(
            @PathVariable String id,
            @RequestBody @Valid RescheduleAppointmentRequest request) {

        // CORREÇÃO: RescheduleAppointmentUseCase exige o Record RescheduleInput
        var input = new RescheduleAppointmentUseCase.RescheduleInput(
                id,
                request.newStartTime());

        return ResponseEntity.ok(rescheduleAppointmentUseCase.execute(input));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam String professionalId,
            @RequestParam String date) {

        // CORREÇÃO: GetAvailableSlotsUseCase exige o Record AvailableSlotsInput
        // Obs: Como o Controller recebe String, precisamos converter para LocalDate.
        // E para os serviços, você precisaria buscar a lista de domínios ou ajustar o
        // Input para IDs.
        var input = new GetAvailableSlotsUseCase.AvailableSlotsInput(
                professionalId,
                LocalDate.parse(date),
                List.of() // Aqui você deve passar os serviços vindos do request ou uma lista vazia para
                          // busca geral
        );

        return ResponseEntity.ok(getAvailableSlotsUseCase.execute(input));
    }
}