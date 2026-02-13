package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sitemap.xml")
@RequiredArgsConstructor
public class SitemapController {

    private final IServiceProviderRepository serviceProviderRepository;

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public String generateSitemap() {
        // Busca todos os estabelecimentos com slug público
        List<ServiceProvider> providers = serviceProviderRepository.findAllWithPublicProfile();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Páginas Estáticas
        addUrl(xml, "https://stylo.app.br/", "1.0", "daily");
        addUrl(xml, "https://stylo.app.br/login", "0.8", "monthly");

        // Páginas Dinâmicas (Perfis)
        // Páginas Dinâmicas (Perfis)
        for (ServiceProvider provider : providers) {
            if (provider.getPublicProfileSlug() != null) {
                // Alterado de getValue() para value()
                String url = "https://stylo.app.br/schedule/" + provider.getPublicProfileSlug().value();
                addUrl(xml, url, "0.9", "weekly");
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String loc, String priority, String freq) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        xml.append("    <changefreq>").append(freq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}