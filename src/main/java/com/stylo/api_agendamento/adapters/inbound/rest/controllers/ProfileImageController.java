package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.UpdateProfilePictureUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/v1/profile-images") // Endpoint base alinhado com o frontend
@RequiredArgsConstructor
@Tag(name = "Imagens de Perfil", description = "Gestão de imagens, logótipos e banners")
public class ProfileImageController {

    private final UpdateProfilePictureUseCase updateProfilePictureUseCase;
    
    // NOTA: Se futuramente criar UseCases específicos para o Banner, pode injetá-los aqui:
    // private final UpdateBannerUseCase updateBannerUseCase;

    @Operation(summary = "Upload de Logótipo do Estabelecimento", description = "Envia uma imagem e atualiza o logótipo do Provider.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logótipo atualizado com sucesso. Retorna o URL público."),
            @ApiResponse(responseCode = "400", description = "Ficheiro inválido, vazio ou formato não suportado."),
            @ApiResponse(responseCode = "401", description = "Acesso não autorizado")
    })
    @PutMapping(value = "/provider/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadProviderLogo(
            @PathVariable String id, // Captura o ID da barbearia/salão que o frontend envia
            @RequestParam("file") MultipartFile file) throws IOException {
        
        return processImageUpload(file);
    }

    @Operation(summary = "Upload de Banner do Estabelecimento", description = "Envia uma imagem e atualiza o banner principal do Provider.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Banner atualizado com sucesso. Retorna o URL público."),
            @ApiResponse(responseCode = "400", description = "Ficheiro inválido, vazio ou formato não suportado."),
            @ApiResponse(responseCode = "401", description = "Acesso não autorizado")
    })
    @PutMapping(value = "/provider/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadProviderBanner(
            @PathVariable String id, // Captura o ID da barbearia/salão
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // Aqui estamos usando o mesmo UseCase para garantir que a imagem sobe para o Firebase.
        // Se a sua regra de negócio exigir guardar na base de dados em colunas diferentes (ex: bannerUrl vs logoUrl),
        // deve chamar um UseCase específico de banner aqui no futuro.
        return processImageUpload(file);
    }

    /**
     * Método auxiliar para centralizar as validações de segurança e upload
     */
    private ResponseEntity<Map<String, String>> processImageUpload(MultipartFile file) throws IOException {
        // 1. Validações básicas de segurança
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "O ficheiro enviado está vazio."));
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Apenas ficheiros de imagem (ex: JPG, PNG) são permitidos."));
        }

        // 2. Executa o UseCase
        String url = updateProfilePictureUseCase.execute(
                file.getInputStream(),
                contentType,
                file.getOriginalFilename(),
                file.getSize() 
        );

        // 3. Retorna exatamente o que o frontend aguarda: { "url": "https://..." }
        return ResponseEntity.ok(Map.of("url", url));
    }
}