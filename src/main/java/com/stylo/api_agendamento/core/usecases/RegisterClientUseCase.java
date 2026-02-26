package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.RegisterClientRequest;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.domain.vo.Document;
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

    @Transactional
    public User execute(RegisterClientRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Este e-mail já está cadastrado em nosso sistema.");
        }

        ClientPhone phoneVo = new ClientPhone(request.phoneNumber());
        
        // Instancia o Documento para validar e limpar a formatação do CPF
        Document documentVo = new Document(request.cpf(), Document.DocumentType.CPF);

        // Cria o Perfil do Cliente extraindo o valor (String) do documentVo
        Client newClient = Client.create(
                request.name(),
                request.email(),
                phoneVo,
                documentVo.value(),
                request.dateOfBirth(), // ✨ AGORA PASSA A DATA
                request.gender()       // ✨ AGORA PASSA O GÊNERO
        );
        Client savedClient = clientRepository.save(newClient);

        User newUser = User.create(
                request.name(),
                request.email(),
                request.phoneNumber(),
                UserRole.CLIENT);

        newUser.changePassword(passwordEncoder.encode(request.password()));
        newUser.linkClient(savedClient.getId());

        if (request.firebaseUid() != null && !request.firebaseUid().isBlank()) {
            newUser.linkFirebase(request.firebaseUid());
        }

        return userRepository.save(newUser);
    }
}