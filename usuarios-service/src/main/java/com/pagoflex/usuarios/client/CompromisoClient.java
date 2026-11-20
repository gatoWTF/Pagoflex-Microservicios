package com.pagoflex.usuarios.client;

import com.pagoflex.usuarios.dto.CompromisoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CompromisoClient {

    private static final Set<String> ESTADOS_POR_PAGAR = Set.of("PENDIENTE", "VENCIDO", "EN_REVISION");

    private final WebClient webClient;

    public CompromisoClient(WebClient.Builder webClientBuilder, @Value("${compromisos.service.url}") String compromisosUrl) {
        this.webClient = webClientBuilder.clone().baseUrl(compromisosUrl).build();
    }

    public void actualizarNombreTitular(String rut, String nombre) {
        try {
            webClient.put()
                    .uri("/api/compromisos/titular/{rut}", rut)
                    .bodyValue(Map.of("nombre", nombre))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("No se pudo actualizar el nombre en compromisos-service: {}", e.getMessage());
        }
    }

    public boolean tieneCuotasPorPagar(String rut) {
        try {
            Boolean tiene = webClient.get()
                    .uri(uri -> uri.path("/api/compromisos").queryParam("rut", rut).build())
                    .retrieve()
                    .bodyToFlux(CompromisoDto.class)
                    .any(compromiso -> ESTADOS_POR_PAGAR.contains(compromiso.estado()))
                    .block();
            return Boolean.TRUE.equals(tiene);
        } catch (WebClientRequestException | WebClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de compromisos no está disponible");
        }
    }
}
