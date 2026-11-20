package com.pagoflex.compromisos.controller;

import com.pagoflex.compromisos.dto.AnularRequest;
import com.pagoflex.compromisos.dto.CambiarEstadoRequest;
import com.pagoflex.compromisos.dto.ComprobanteResponse;
import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.dto.GestionReporteRequest;
import com.pagoflex.compromisos.dto.NombreTitularRequest;
import com.pagoflex.compromisos.dto.NuevoCompromisoRequest;
import com.pagoflex.compromisos.dto.NuevoReporteRequest;
import com.pagoflex.compromisos.dto.PagoRequest;
import com.pagoflex.compromisos.dto.PagoResponse;
import com.pagoflex.compromisos.dto.ReporteResponse;
import com.pagoflex.compromisos.model.CanalPago;
import com.pagoflex.compromisos.model.EstadoCompromiso;
import com.pagoflex.compromisos.model.MotivoAnulacion;
import com.pagoflex.compromisos.service.CompromisoService;
import com.pagoflex.compromisos.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControladoresTest {

    @Mock
    private CompromisoService compromisoService;

    @Mock
    private ReporteService reporteService;

    @InjectMocks
    private CompromisoController compromisoController;

    @InjectMocks
    private ComprobanteController comprobanteController;

    @InjectMocks
    private ReporteController reporteController;

    private final CompromisoResponse compromiso = mock(CompromisoResponse.class);

    @Test
    void listarCompromisos_conRut_buscaPorUsuario() {
        when(compromisoService.listarPorUsuario("15.834.207-3")).thenReturn(List.of(compromiso));

        assertEquals(List.of(compromiso), compromisoController.listar("15.834.207-3", null));
    }

    @Test
    void listarCompromisos_conEmpresa_buscaPorEmpresa() {
        when(compromisoService.listarPorEmpresa("EC-06")).thenReturn(List.of(compromiso));

        assertEquals(List.of(compromiso), compromisoController.listar(" ", "EC-06"));
    }

    @Test
    void listarCompromisos_sinRutNiEmpresa_lanzaError() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> compromisoController.listar(null, ""));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void obtenerCompromiso_devuelveElDelServicio() {
        when(compromisoService.obtener("CP-2026-0102")).thenReturn(compromiso);

        assertEquals(compromiso, compromisoController.obtener("CP-2026-0102"));
    }

    @Test
    void registrarCompromiso_entregaLosDatosAlServicio() {
        NuevoCompromisoRequest datos = new NuevoCompromisoRequest("18.704.396-4", "Benjamin Araya", "EC-03",
                "Crédito de consumo", 12, 1, 86400, "25-12-2026", "EJ-03");
        when(compromisoService.registrar(datos)).thenReturn(compromiso);

        assertEquals(compromiso, compromisoController.registrar(datos));
    }

    @Test
    void cambiarEstado_entregaElEstadoAlServicio() {
        CambiarEstadoRequest datos = new CambiarEstadoRequest(EstadoCompromiso.EN_REVISION);
        when(compromisoService.cambiarEstado("CP-2026-0102", datos)).thenReturn(compromiso);

        assertEquals(compromiso, compromisoController.cambiarEstado("CP-2026-0102", datos));
    }

    @Test
    void anular_entregaElMotivoAlServicio() {
        AnularRequest datos = new AnularRequest(MotivoAnulacion.REPACTACION, null, "EJ-05", "EC-06");
        when(compromisoService.anular("CP-2026-0102", datos)).thenReturn(compromiso);

        assertEquals(compromiso, compromisoController.anular("CP-2026-0102", datos));
    }

    @Test
    void actualizarNombreTitular_llamaAlServicio() {
        NombreTitularRequest datos = new NombreTitularRequest("Camila Rojas");

        compromisoController.actualizarNombreTitular("15.834.207-3", datos);

        verify(compromisoService).actualizarNombreTitular("15.834.207-3", datos);
    }

    @Test
    void pagar_devuelveElComprobanteDelServicio() {
        PagoRequest datos = new PagoRequest(CanalPago.APP, "Pago simulado");
        PagoResponse pago = new PagoResponse(mock(ComprobanteResponse.class), null);
        when(compromisoService.pagar("CP-2026-0102", datos)).thenReturn(pago);

        assertEquals(pago, compromisoController.pagar("CP-2026-0102", datos));
    }

    @Test
    void listarComprobantes_devuelveLosDelServicio() {
        ComprobanteResponse comprobante = mock(ComprobanteResponse.class);
        when(compromisoService.listarComprobantes("15.834.207-3")).thenReturn(List.of(comprobante));

        assertEquals(List.of(comprobante), comprobanteController.listar("15.834.207-3"));
    }

    @Test
    void reportes_cadaAccionLlamaAlServicio() {
        ReporteResponse reporte = mock(ReporteResponse.class);
        NuevoReporteRequest nuevo = new NuevoReporteRequest("CP-2026-0102", "15.834.207-3", "Otro", "Comentario");
        GestionReporteRequest gestion = new GestionReporteRequest("AG-01", "Listo");
        when(reporteService.listar(null)).thenReturn(List.of(reporte));
        when(reporteService.crear(nuevo)).thenReturn(reporte);
        when(reporteService.tomar("REP-0013", gestion)).thenReturn(reporte);
        when(reporteService.resolver("REP-0013", gestion)).thenReturn(reporte);

        assertEquals(List.of(reporte), reporteController.listar(null));
        assertEquals(reporte, reporteController.crear(nuevo));
        assertEquals(reporte, reporteController.tomar("REP-0013", gestion));
        assertEquals(reporte, reporteController.resolver("REP-0013", gestion));
    }
}
