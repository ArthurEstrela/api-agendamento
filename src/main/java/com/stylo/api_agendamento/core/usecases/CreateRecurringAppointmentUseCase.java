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
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateRecurringAppointmentUseCase {

    private final CreateAppointmentUseCase createAppointmentUseCase;

    public Response execute(Input input) {
        List<Appointment> successes = new ArrayList<>();
        List<FailedOccurrence> failures = new ArrayList<>();

        LocalDateTime currentSlot = input.firstStartTime();
        int count = 0;
        
        // Limite de segurança: se não houver occurrences ou endDate, limita a 1 ano (52 semanas)
        int maxOccurrences = input.occurrences() != null ? input.occurrences() : 52;

        log.info("Iniciando criação de agendamento recorrente ({}) para o cliente {}", 
                input.recurrenceType(), input.clientId());

        while (count < maxOccurrences) {
            // Verifica se ultrapassou a data limite definida pelo usuário
            if (input.endDate() != null && currentSlot.isAfter(input.endDate())) {
                break;
            }

            try {
                // Reutilizamos o caso de uso de agendamento individual para herdar Lock e Validações
                var singleInput = new CreateAppointmentUseCase.Input(
                        input.clientId(),
                        input.professionalId(),
                        input.serviceIds(),
                        currentSlot,
                        input.reminderMinutes(),
                        null, // Cupom não aplicado em recorrência para evitar exaustão de limites
                        "Recorrência: " + (count + 1) // Notas automáticas
                );

                Appointment appt = createAppointmentUseCase.execute(singleInput);
                successes.add(appt);

            } catch (Exception e) {
                log.warn("Falha na ocorrência {} em {}: {}", count + 1, currentSlot, e.getMessage());
                failures.add(new FailedOccurrence(currentSlot, e.getMessage()));
            }

            // Calcula o próximo passo na linha do tempo
            currentSlot = calculateNextSlot(currentSlot, input.recurrenceType());
            count++;
        }

        return new Response(successes, failures);
    }

    private LocalDateTime calculateNextSlot(LocalDateTime current, RecurrenceType type) {
        if (type == null) return current.plusWeeks(1);
        
        return switch (type) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    @Builder
    public record Input(
            UUID clientId,
            UUID professionalId,
            List<UUID> serviceIds,
            LocalDateTime firstStartTime,
            Integer reminderMinutes,
            RecurrenceType recurrenceType,
            Integer occurrences,
            LocalDateTime endDate
    ) {}

    public record Response(
            List<Appointment> createdAppointments,
            List<FailedOccurrence> failedOccurrences
    ) {
        public boolean hasFailures() { return !failedOccurrences.isEmpty(); }

        public String getSummary() {
            if (failedOccurrences.isEmpty()) {
                return "Sucesso: " + createdAppointments.size() + " agendamentos criados.";
            }
            return String.format("Concluídos: %d | Falhas: %d. Alguns horários estavam ocupados.", 
                    createdAppointments.size(), failedOccurrences.size());
        }
    }

    public record FailedOccurrence(LocalDateTime date, String reason) {}
}