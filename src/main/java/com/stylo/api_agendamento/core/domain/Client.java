package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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

    // ✨ OTIMIZAÇÃO: Usando Set para garantir unicidade e melhor performance no JPA
    @Builder.Default
    private Set<UUID> favoriteProviders = new HashSet<>(); // Barbearias/Salões

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
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .phoneNumber(phone)
                .cpf(cpf)
                .favoriteProviders(new HashSet<>())
                .noShowCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- ENCAPSULAMENTO DE LISTAS (DDD Puro) ---

    public Set<UUID> getFavoriteProviders() {
        return favoriteProviders != null
                ? Collections.unmodifiableSet(favoriteProviders)
                : Collections.emptySet();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void addFavoriteProvider(UUID providerId) {
        if (providerId == null)
            throw new BusinessException("ID do estabelecimento inválido.");

        if (this.favoriteProviders == null)
            this.favoriteProviders = new HashSet<>();

        // O método .add() do Set retorna true apenas se o elemento não existia antes
        if (this.favoriteProviders.add(providerId)) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeFavoriteProvider(UUID providerId) {
        if (this.favoriteProviders != null && providerId != null) {
            // O método .remove() retorna true se realmente removeu algo
            if (this.favoriteProviders.remove(providerId)) {
                this.updatedAt = LocalDateTime.now();
            }
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Client client = (Client) o;
        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}