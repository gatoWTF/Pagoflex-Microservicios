package com.pagoflex.usuarios.client;

import com.pagoflex.usuarios.dto.RolDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientesTest {

    private final List<ClientRequest> peticiones = new ArrayList<>();

    private WebClient.Builder servidorQueResponde(HttpStatus estado, String json) {
        return WebClient.builder().exchangeFunction(peticion -> {
            peticiones.add(peticion);
            return Mono.just(ClientResponse.create(estado)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(json)
                    .build());
        });
    }

    private WebClient.Builder servidorApagado() {
        return WebClient.builder().exchangeFunction(peticion -> Mono.error(new WebClientRequestException(
                new ConnectException("Conexion rechazada"), peticion.method(), peticion.url(), new HttpHeaders())));
    }

    @Test
    void obtenerRol_conRolExistente_devuelveSuNombre() {
        RolClient cliente = new RolClient(servidorQueResponde(HttpStatus.OK, "{\"id\":2,\"nombre\":\"Ejecutivo de empresa\"}"),
                "http://roles-service");

        RolDto rol = cliente.obtener(2L);

        assertEquals("Ejecutivo de empresa", rol.nombre());
        assertEquals(URI.create("http://roles-service/api/roles/2"), peticiones.get(0).url());
    }

    @Test
    void obtenerRol_conRolInexistente_devuelveNulo() {
        RolClient cliente = new RolClient(servidorQueResponde(HttpStatus.NOT_FOUND, "{}"), "http://roles-service");

        assertNull(cliente.obtener(9L));
    }

    @Test
    void obtenerRol_conServicioApagado_lanzaServicioNoDisponible() {
        RolClient cliente = new RolClient(servidorApagado(), "http://roles-service");

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> cliente.obtener(1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertEquals("El servicio de roles no está disponible", error.getReason());
    }

    @Test
    void obtenerRol_conServicioSinInstancias_lanzaServicioNoDisponible() {
        RolClient cliente = new RolClient(servidorQueResponde(HttpStatus.SERVICE_UNAVAILABLE, "{}"), "http://roles-service");

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> cliente.obtener(1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void actualizarNombreTitular_enviaPutConElRut() {
        CompromisoClient cliente = new CompromisoClient(servidorQueResponde(HttpStatus.NO_CONTENT, ""),
                "http://compromisos-service");

        cliente.actualizarNombreTitular("15.834.207-3", "Camila Rojas");

        assertEquals(HttpMethod.PUT, peticiones.get(0).method());
        assertEquals("/api/compromisos/titular/15.834.207-3", peticiones.get(0).url().getPath());
    }

    @Test
    void tieneCuotasPorPagar_conUnaCuotaVencida_devuelveVerdadero() {
        String json = "[{\"folio\":\"CP-0101\",\"estado\":\"PAGADO\",\"monto\":15000},"
                + "{\"folio\":\"CP-0102\",\"estado\":\"VENCIDO\",\"monto\":15000}]";
        CompromisoClient cliente = new CompromisoClient(servidorQueResponde(HttpStatus.OK, json),
                "http://compromisos-service");

        assertTrue(cliente.tieneCuotasPorPagar("15.834.207-3"));
        assertEquals(HttpMethod.GET, peticiones.get(0).method());
        assertEquals("/api/compromisos", peticiones.get(0).url().getPath());
        assertEquals("rut=15.834.207-3", peticiones.get(0).url().getQuery());
    }

    @Test
    void tieneCuotasPorPagar_conCuotasPagadasYAnuladas_devuelveFalso() {
        String json = "[{\"folio\":\"CP-0101\",\"estado\":\"PAGADO\"},{\"folio\":\"CP-0111\",\"estado\":\"ANULADO\"}]";
        CompromisoClient cliente = new CompromisoClient(servidorQueResponde(HttpStatus.OK, json),
                "http://compromisos-service");

        assertFalse(cliente.tieneCuotasPorPagar("15.834.207-3"));
    }

    @Test
    void tieneCuotasPorPagar_sinCompromisos_devuelveFalso() {
        CompromisoClient cliente = new CompromisoClient(servidorQueResponde(HttpStatus.OK, "[]"),
                "http://compromisos-service");

        assertFalse(cliente.tieneCuotasPorPagar("15.834.207-3"));
    }

    @Test
    void tieneCuotasPorPagar_conServicioApagado_lanzaServicioNoDisponible() {
        CompromisoClient cliente = new CompromisoClient(servidorApagado(), "http://compromisos-service");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> cliente.tieneCuotasPorPagar("15.834.207-3"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertEquals("El servicio de compromisos no está disponible", error.getReason());
    }

    @Test
    void tieneCuotasPorPagar_conServicioSinInstancias_lanzaServicioNoDisponible() {
        CompromisoClient cliente = new CompromisoClient(servidorQueResponde(HttpStatus.SERVICE_UNAVAILABLE, "{}"),
                "http://compromisos-service");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> cliente.tieneCuotasPorPagar("15.834.207-3"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void actualizarNombreTitular_conServicioApagado_noLanzaError() {
        CompromisoClient cliente = new CompromisoClient(servidorApagado(), "http://compromisos-service");

        assertDoesNotThrow(() -> cliente.actualizarNombreTitular("15.834.207-3", "Camila Rojas"));
    }
}
