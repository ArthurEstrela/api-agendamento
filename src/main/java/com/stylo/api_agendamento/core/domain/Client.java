package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Client {

    private UUID id;
    private String name;
    private String email;
    private ClientPhone phoneNumber;
    private String cpf;
    private LocalDate dateOfBirth;
    private String gender;

    @Builder.Default
    private List<UUID> favoriteProfessionals = new ArrayList<>();

    private int noShowCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Client create(String name, String email, ClientPhone phone, String cpf) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("O nome do cliente é obrigatório.");
        }
        
        if (email == null || !email.contains("@")) {
            throw new BusinessException("E-mail de cliente inválido.");
        }

        return Client.builder()
                .id(UUID.randomUUID()) // ✨ Gera identidade ao nascer
                .name(name)
                .email(email)
                .phoneNumber(phone)
                .cpf(cpf)
                .favoriteProfessionals(new ArrayList<>()) // ✨ Lista mutável para evitar erro
                .noShowCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void addFavoriteProfessional(UUID professionalId) {
        if (this.favoriteProfessionals == null) {
            this.favoriteProfessionals = new ArrayList<>();
        }
        if (!this.favoriteProfessionals.contains(professionalId)) {
            this.favoriteProfessionals.add(professionalId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeFavoriteProfessional(UUID professionalId) {
        if (this.favoriteProfessionals != null) {
            this.favoriteProfessionals.remove(professionalId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void updateContact(String name, ClientPhone phone) {
        if (name == null || name.isBlank())
            throw new BusinessException("Nome inválido para atualização.");
        
        this.name = name;
        this.phoneNumber = phone;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementNoShow() {
        this.noShowCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void resetNoShow() {
        this.noShowCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isBlockedByNoShow(int limit) {
        return this.noShowCount >= limit;
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}