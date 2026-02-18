package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.vo.RecurrenceType;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateRecurringAppointmentUseCase {

    private final CreateAppointmentUseCase createAppointmentUseCase;

    public RecurringResponse execute(RecurringInput input) {
        List<Appointment> successes = new ArrayList<>();
        List<FailedAppointment> failures = new ArrayList<>();

        LocalDateTime currentSlot = input.firstStartTime();
        int count = 0;
        int maxOccurrences = input.occurrences() != null ? input.occurrences() : 52;

        while (count < maxOccurrences) {
            if (input.endDate() != null && currentSlot.isAfter(input.endDate())) {
                break;
            }

            try {
                // ✨ CORREÇÃO: Adicionado o parâmetro 'null' para o couponCode
                var singleInput = new CreateAppointmentUseCase.CreateAppointmentInput(
                        input.clientId(),
                        input.professionalId(),
                        input.serviceIds(),
                        currentSlot,
                        input.reminderMinutes(),
                        null // Cupom não aplicado automaticamente em recorrência para evitar bloqueio por
                             // limite de uso
                );

                Appointment appt = createAppointmentUseCase.execute(singleInput);

                successes.add(appt);
                log.info("Agendamento recorrente {}/{} criado para {}", count + 1, maxOccurrences, currentSlot);

            } catch (Exception e) {
                log.warn("Falha ao criar recorrência em {}: {}", currentSlot, e.getMessage());
                failures.add(new FailedAppointment(currentSlot, e.getMessage()));
            }

            currentSlot = calculateNextSlot(currentSlot, input.recurrenceType());
            count++;
        }

        return new RecurringResponse(successes, failures);
    }

    private LocalDateTime calculateNextSlot(LocalDateTime current, RecurrenceType type) {
        if (type == null)
            return current.plusDays(1); // Default safe
        return switch (type) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    @Builder
    public record RecurringInput(
            String clientId,
            String professionalId,
            List<String> serviceIds,
            LocalDateTime firstStartTime,
            Integer reminderMinutes,
            RecurrenceType recurrenceType,
            Integer occurrences,
            LocalDateTime endDate) {
    }

    public record RecurringResponse(
            List<Appointment> createdAppointments,
            List<FailedAppointment> failedAppointments) {
        public boolean hasFailures() {
            return !failedAppointments.isEmpty();
        }

        public String getSummaryMessage() {
            if (failedAppointments.isEmpty()) {
                return "Todos os " + createdAppointments.size() + " agendamentos foram criados com sucesso.";
            }
            return String.format("Agendados: %d. Falhas: %d. Verifique os horários indisponíveis.",
                    createdAppointments.size(), failedAppointments.size());
        }
    }

    public record FailedAppointment(
            LocalDateTime date,
            String reason) {
    }
}