package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public record Slug(String value) {
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public Slug {
        if (value == null || !value.matches("^[a-z0-9-]+$")) {
            throw new BusinessException("Slug inválido. Use apenas letras minúsculas, números e hífens.");
        }
    }

    /**
     * Gera um Slug válido a partir de um texto qualquer.
     * Ex: "Salão Beleza & Estilo" -> "salao-beleza-estilo"
     */
    public static Slug createFromText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("Texto para gerar slug não pode ser vazio.");
        }

        String nowhitespace = WHITESPACE.matcher(text).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        
        // Remove hífens duplicados e converte para minúsculo
        slug = slug.replaceAll("-{2,}", "-").toLowerCase(Locale.ENGLISH);
        
        // Remove hífen do começo ou fim
        slug = slug.replaceAll("^-|-$", "");

        return new Slug(slug);
    }
}