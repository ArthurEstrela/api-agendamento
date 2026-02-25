package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.RegisterClientRequest;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterClientUseCase {

    private final IUserRepository userRepository;
    private final IClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional // ✨ MUITO IMPORTANTE: Garante que se falhar ao salvar o User, ele desfaz o
                   // Client
    public User execute(RegisterClientRequest request) {

        // 1. Valida se o e-mail já existe
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Este e-mail já está cadastrado em nosso sistema.");
        }

        // 2. Cria o Perfil do Cliente (Tabela clients)
        // Nota: Assumindo que seu VO ClientPhone pode ser instanciado assim. Adapte se
        // for um método estático (.of)
        ClientPhone phoneVo = new ClientPhone(request.phoneNumber());

        Client newClient = Client.create(
                request.name(),
                request.email(),
                phoneVo,
                null // O CPF pode ser nulo no cadastro inicial e preenchido no perfil depois
        );
        Client savedClient = clientRepository.save(newClient);

        // 3. Cria a conta de Autenticação (Tabela users)
        User newUser = User.create(
                request.name(),
                request.email(),
                request.phoneNumber(),
                UserRole.CLIENT);

        // 4. Configura senha e VINCULA o ID do cliente recém-criado
        newUser.changePassword(passwordEncoder.encode(request.password()));
        newUser.linkClient(savedClient.getId());

        if (request.firebaseUid() != null && !request.firebaseUid().isBlank()) {
            newUser.linkFirebase(request.firebaseUid());
        }

        // 5. Salva o usuário e retorna
        return userRepository.save(newUser);
    }
}