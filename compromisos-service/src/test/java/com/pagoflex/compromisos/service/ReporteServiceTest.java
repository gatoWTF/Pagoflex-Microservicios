package com.pagoflex.compromisos.service;

import com.pagoflex.compromisos.dto.CambiarEstadoRequest;
import com.pagoflex.compromisos.dto.GestionReporteRequest;
import com.pagoflex.compromisos.dto.NuevoReporteRequest;
import com.pagoflex.compromisos.dto.ReporteResponse;
import com.pagoflex.compromisos.model.Compromiso;
import com.pagoflex.compromisos.model.Empresa;
import com.pagoflex.compromisos.model.EstadoCompromiso;
import com.pagoflex.compromisos.model.EstadoReporte;
import com.pagoflex.compromisos.model.Reporte;
import com.pagoflex.compromisos.repository.CompromisoRepository;
import com.pagoflex.compromisos.repository.EmpresaRepository;
import com.pagoflex.compromisos.repository.ReporteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private CompromisoRepository compromisoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private CompromisoService compromisoService;

    @InjectMocks
    private ReporteService reporteService;

    private Compromiso cuotaDeCamila(EstadoCompromiso estado) {
        return Compromiso.builder()
                .folio("CP-2026-0102")
                .rutUsuario("15.834.207-3")
                .rutClave("158342073")
                .nombreUsuario("Camila Rojas")
                .empresaCodigo("EC-06")
                .concepto("Cuota social")
                .cuotaActual(10)
                .totalCuotas(12)
                .monto(15000)
                .recargoAplicado(2000)
                .estado(estado)
                .build();
    }

    private Reporte reporte(String numero, EstadoReporte estado, String agente) {
        return Reporte.builder()
                .numero(numero)
                .compromisoFolio("CP-2026-0102")
                .rutUsuario("15.834.207-3")
                .rutClave("158342073")
                .motivo("El monto no corresponde")
                .comentario("")
                .fecha("05-10-2026")
                .estado(estado)
                .agenteResponsable(agente)
                .build();
    }

    private void compromisoExiste(Compromiso compromiso) {
        when(compromisoRepository.findByFolio("CP-2026-0102")).thenReturn(Optional.of(compromiso));
        when(empresaRepository.findById("EC-06")).thenReturn(Optional.of(new Empresa("EC-06", "Club Los Halcones", 2000)));
    }

    private void guardarDevuelveLoMismo() {
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private ResponseStatusException esperarError(Runnable accion) {
        return assertThrows(ResponseStatusException.class, accion::run);
    }

    @Test
    void listar_sinRut_muestraPrimeroLosAbiertosYLosMasNuevos() {
        when(reporteRepository.findAll()).thenReturn(List.of(
                reporte("REP-0001", EstadoReporte.RESUELTO, "AG-01"),
                reporte("REP-0002", EstadoReporte.ABIERTO, null),
                reporte("REP-0003", EstadoReporte.ABIERTO, null)));
        when(compromisoRepository.findByFolio(anyString())).thenReturn(Optional.empty());

        List<ReporteResponse> resultado = reporteService.listar(null);

        assertEquals("REP-0003", resultado.get(0).numero());
        assertEquals("REP-0002", resultado.get(1).numero());
        assertEquals("REP-0001", resultado.get(2).numero());
        assertNull(resultado.get(0).empresaNombre());
    }

    @Test
    void listar_conRut_agregaLosDatosDelCompromiso() {
        when(reporteRepository.findByRutClave("158342073")).thenReturn(List.of(reporte("REP-0012", EstadoReporte.ABIERTO, null)));
        compromisoExiste(cuotaDeCamila(EstadoCompromiso.EN_REVISION));

        List<ReporteResponse> resultado = reporteService.listar("15.834.207-3");

        assertEquals("Club Los Halcones", resultado.get(0).empresaNombre());
        assertEquals(17000, resultado.get(0).montoCuota());
        assertEquals(EstadoCompromiso.EN_REVISION, resultado.get(0).estadoCompromiso());
    }

    @Test
    void crear_sinDatos_lanzaError() {
        ResponseStatusException error = esperarError(() -> reporteService.crear(null));

        assertEquals("Folio, RUT y motivo son obligatorios", error.getReason());
    }

    @Test
    void crear_conFolioInexistente_lanzaNoEncontrado() {
        when(compromisoRepository.findByFolio("CP-0000")).thenReturn(Optional.empty());

        ResponseStatusException error = esperarError(() -> reporteService.crear(
                new NuevoReporteRequest("CP-0000", "15.834.207-3", "Otro", null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void crear_porQuienNoEsElTitular_lanzaProhibido() {
        when(compromisoRepository.findByFolio("CP-2026-0102")).thenReturn(Optional.of(cuotaDeCamila(EstadoCompromiso.PENDIENTE)));

        ResponseStatusException error = esperarError(() -> reporteService.crear(
                new NuevoReporteRequest("CP-2026-0102", "18.704.396-4", "Otro", null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("Solo el titular del compromiso puede reportarlo", error.getReason());
    }

    @Test
    void crear_sobreCompromisoPagado_lanzaConflicto() {
        when(compromisoRepository.findByFolio("CP-2026-0102")).thenReturn(Optional.of(cuotaDeCamila(EstadoCompromiso.PAGADO)));

        ResponseStatusException error = esperarError(() -> reporteService.crear(
                new NuevoReporteRequest("CP-2026-0102", "15.834.207-3", "Otro", null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void crear_conDatosCorrectos_abreElReporteYPoneElCompromisoEnRevision() {
        compromisoExiste(cuotaDeCamila(EstadoCompromiso.VENCIDO));
        when(reporteRepository.findAll()).thenReturn(List.of(reporte("REP-0012", EstadoReporte.ABIERTO, null)));
        guardarDevuelveLoMismo();

        ReporteResponse resultado = reporteService.crear(
                new NuevoReporteRequest(" CP-2026-0102 ", "15834207-3", " El monto no corresponde ", null));

        assertEquals("REP-0013", resultado.numero());
        assertEquals(EstadoReporte.ABIERTO, resultado.estado());
        assertEquals("El monto no corresponde", resultado.motivo());
        assertEquals("", resultado.comentario());
        verify(compromisoService).cambiarEstado("CP-2026-0102", new CambiarEstadoRequest(EstadoCompromiso.EN_REVISION));
    }

    @Test
    void tomar_sinAgente_lanzaError() {
        ResponseStatusException error = esperarError(() -> reporteService.tomar("REP-0012", null));

        assertEquals("Indica el agente que gestiona el reporte", error.getReason());
    }

    @Test
    void tomar_reporteInexistente_lanzaNoEncontrado() {
        when(reporteRepository.findByNumero("REP-0099")).thenReturn(Optional.empty());

        ResponseStatusException error = esperarError(
                () -> reporteService.tomar("REP-0099", new GestionReporteRequest("AG-01", null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void tomar_reporteYaTomado_lanzaConflicto() {
        when(reporteRepository.findByNumero("REP-0011")).thenReturn(Optional.of(reporte("REP-0011", EstadoReporte.EN_GESTION, "AG-02")));

        ResponseStatusException error = esperarError(
                () -> reporteService.tomar("REP-0011", new GestionReporteRequest("AG-01", null)));

        assertEquals("Este reporte ya fue tomado por AG-02", error.getReason());
    }

    @Test
    void tomar_reporteAbierto_quedaEnGestionDelAgente() {
        when(reporteRepository.findByNumero("REP-0012")).thenReturn(Optional.of(reporte("REP-0012", EstadoReporte.ABIERTO, null)));
        guardarDevuelveLoMismo();
        compromisoExiste(cuotaDeCamila(EstadoCompromiso.EN_REVISION));

        ReporteResponse resultado = reporteService.tomar("REP-0012", new GestionReporteRequest(" AG-01 ", null));

        assertEquals(EstadoReporte.EN_GESTION, resultado.estado());
        assertEquals("AG-01", resultado.agenteResponsable());
    }

    @Test
    void resolver_reporteYaResuelto_lanzaConflicto() {
        when(reporteRepository.findByNumero("REP-0005")).thenReturn(Optional.of(reporte("REP-0005", EstadoReporte.RESUELTO, "AG-01")));

        ResponseStatusException error = esperarError(
                () -> reporteService.resolver("REP-0005", new GestionReporteRequest("AG-01", null)));

        assertEquals("El reporte ya está resuelto", error.getReason());
    }

    @Test
    void resolver_reporteSinTomar_lanzaConflicto() {
        when(reporteRepository.findByNumero("REP-0012")).thenReturn(Optional.of(reporte("REP-0012", EstadoReporte.ABIERTO, null)));

        ResponseStatusException error = esperarError(
                () -> reporteService.resolver("REP-0012", new GestionReporteRequest("AG-01", null)));

        assertEquals("Toma el reporte antes de resolverlo", error.getReason());
    }

    @Test
    void resolver_porOtroAgente_lanzaProhibido() {
        when(reporteRepository.findByNumero("REP-0011")).thenReturn(Optional.of(reporte("REP-0011", EstadoReporte.EN_GESTION, "AG-02")));

        ResponseStatusException error = esperarError(
                () -> reporteService.resolver("REP-0011", new GestionReporteRequest("AG-01", null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("Este reporte lo gestiona AG-02", error.getReason());
    }

    @Test
    void resolver_porSuAgente_cierraElReporteYDevuelveElCompromiso() {
        when(reporteRepository.findByNumero("REP-0011")).thenReturn(Optional.of(reporte("REP-0011", EstadoReporte.EN_GESTION, "AG-01")));
        guardarDevuelveLoMismo();
        compromisoExiste(cuotaDeCamila(EstadoCompromiso.EN_REVISION));

        ReporteResponse resultado = reporteService.resolver("REP-0011", new GestionReporteRequest("AG-01", " "));

        assertEquals(EstadoReporte.RESUELTO, resultado.estado());
        assertEquals("Gestionado por soporte.", resultado.respuesta());
        verify(compromisoService).cambiarEstado("CP-2026-0102", new CambiarEstadoRequest(EstadoCompromiso.PENDIENTE));
    }
}
