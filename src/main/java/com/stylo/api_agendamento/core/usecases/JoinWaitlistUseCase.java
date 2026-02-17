package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.Waitlist;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.ports.IWaitlistRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@UseCase
@RequiredArgsConstructor
public class JoinWaitlistUseCase {

    private final IWaitlistRepository waitlistRepository;
    private final IUserRepository userRepository;

    public void execute(JoinInput input) {
        if (input.date().isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível entrar na lista de espera de uma data passada.");
        }

        User client = userRepository.findById(input.clientId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

        Waitlist entry = Waitlist.builder()
                .professionalId(input.professionalId())
                .clientId(client.getId())
                .clientName(client.getName())
                .clientEmail(client.getEmail())
                .clientPhone(client.getPhoneNumber())
                .desiredDate(input.date())
                .requestTime(LocalDateTime.now())
                .notified(false)
                .build();

        waitlistRepository.save(entry);
    }

    public record JoinInput(String clientId, String professionalId, LocalDate date) {}
}