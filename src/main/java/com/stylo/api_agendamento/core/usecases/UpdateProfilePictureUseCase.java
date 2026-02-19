package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.ports.IStorageProvider;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class UpdateProfilePictureUseCase {

    private final IUserRepository userRepository;
    private final IStorageProvider storageProvider;
    private final IUserContext userContext;

    @Transactional
    public String execute(InputStream fileContent, String contentType, String originalFilename, long size) {
        User user = userContext.getCurrentUser();
        
        // 1. Deletar imagem antiga se existir (Evita lixo no Bucket)
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) {
            try {
                storageProvider.deleteFile(user.getProfilePictureUrl());
            } catch (Exception e) {
                log.warn("Falha não-bloqueante ao deletar imagem antiga do usuário {}: {}", user.getId(), e.getMessage());
            }
        }

        // 2. Definir novo caminho padronizado: profiles/{userId}/{timestamp}.ext
        String extension = getExtension(originalFilename);
        String path = String.format("profiles/%s/%d.%s", user.getId(), System.currentTimeMillis(), extension);

        // 3. Upload através do StorageProvider (Adapter S3/Firebase)
        String publicUrl = storageProvider.uploadFile(path, fileContent, contentType, size);

        // 4. Atualizar registro do usuário
        user.updateAvatar(publicUrl);
        userRepository.save(user);

        log.info("Foto de perfil atualizada com sucesso para o usuário: {}", user.getId());
        return publicUrl;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}