package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase; // ✨ Import da sua anotação personalizada
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.ports.IStorageProvider;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

@UseCase // ✨ Substitui @Service para manter a semântica da Clean Arch
@RequiredArgsConstructor
public class UpdateProfilePictureUseCase {

    private final IUserRepository userRepository;
    private final IStorageProvider storageProvider;
    private final IUserContext userContext;

    @Transactional
    public String execute(InputStream fileContent, String contentType, String originalFilename) {
        User user = userContext.getCurrentUser();
        
        // 1. Definir o caminho do arquivo (Padronização é chave para organização)
        // ex: profiles/user-uuid-timestamp.jpg
        String extension = getExtension(originalFilename);
        String newFileName = String.format("profiles/%s-%d.%s", user.getId(), System.currentTimeMillis(), extension);

        // 2. Upload (Adapter do Firebase entra em ação aqui)
        String publicUrl = storageProvider.uploadFile(newFileName, fileContent, contentType);

        // 3. Atualizar usuário (e deletar imagem antiga se necessário - lógica futura)
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) {
            // TODO: Futuramente implementar storageProvider.deleteFile(...) aqui
        }

        user.updateProfile(user.getName(), user.getPhoneNumber(), publicUrl);
        userRepository.save(user);

        return publicUrl;
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return "jpg";
        return filename.substring(lastDot + 1);
    }
}