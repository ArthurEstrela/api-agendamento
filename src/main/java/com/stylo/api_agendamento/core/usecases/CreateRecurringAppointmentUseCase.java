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
@UseCase // Injeção automática (sem BeanConfiguration)
@RequiredArgsConstructor
public class CreateRecurringAppointmentUseCase {

    // Reutilizamos o UseCase unitário para garantir integridade, locks e eventos
    private final CreateAppointmentUseCase createAppointmentUseCase;

    public RecurringResponse execute(RecurringInput input) {
        List<Appointment> successes = new ArrayList<>();
        List<FailedAppointment> failures = new ArrayList<>();

        LocalDateTime currentSlot = input.firstStartTime();
        int count = 0;

        // Proteção contra loops infinitos (máximo 52 ocorrências = 1 ano semanal)
        int maxOccurrences = input.occurrences() != null ? input.occurrences() : 52;
        
        // Loop de Agendamento
        while (count < maxOccurrences) {
            // Condição de parada por data
            if (input.endDate() != null && currentSlot.isAfter(input.endDate())) {
                break;
            }

            try {
                // Monta o input para o agendamento unitário
                var singleInput = new CreateAppointmentUseCase.CreateAppointmentInput(
                        input.clientId(),
                        input.professionalId(),
                        input.serviceIds(),
                        currentSlot,
                        input.reminderMinutes()
                );

                // ✨ AQUI A MÁGICA:
                // Chamamos o UseCase unitário. Ele vai:
                // 1. Abrir transação e Lock no banco
                // 2. Validar horário e regras
                // 3. Salvar
                // 4. Disparar evento pro Google Calendar (Async)
                Appointment appt = createAppointmentUseCase.execute(singleInput);
                
                successes.add(appt);
                log.info("Agendamento recorrente {}/{} criado para {}", count + 1, maxOccurrences, currentSlot);

            } catch (Exception e) {
                // Se falhar (ex: horário ocupado, feriado, bloqueio), registramos o erro
                // mas NÃO paramos o loop. O cliente quer marcar os que derem certo.
                log.warn("Falha ao criar recorrência em {}: {}", currentSlot, e.getMessage());
                failures.add(new FailedAppointment(currentSlot, e.getMessage()));
            }

            // Calcula a próxima data
            currentSlot = calculateNextSlot(currentSlot, input.recurrenceType());
            count++;
        }

        return new RecurringResponse(successes, failures);
    }

    private LocalDateTime calculateNextSlot(LocalDateTime current, RecurrenceType type) {
        return switch (type) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    // --- DTOs de Entrada e Saída ---

    @Builder
    public record RecurringInput(
            String clientId,
            String professionalId,
            List<String> serviceIds,
            LocalDateTime firstStartTime, // Data do primeiro agendamento
            Integer reminderMinutes,
            RecurrenceType recurrenceType,
            Integer occurrences, // Opção A: "Repetir 10 vezes"
            LocalDateTime endDate    // Opção B: "Repetir até o fim do ano"
    ) {}

    public record RecurringResponse(
            List<Appointment> createdAppointments,
            List<FailedAppointment> failedAppointments
    ) {
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
            String reason
    ) {}
}