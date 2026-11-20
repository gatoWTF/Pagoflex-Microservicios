package com.pagoflex.compromisos.service;

import com.pagoflex.compromisos.dto.AnularRequest;
import com.pagoflex.compromisos.dto.CambiarEstadoRequest;
import com.pagoflex.compromisos.dto.ComprobanteResponse;
import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.dto.NombreTitularRequest;
import com.pagoflex.compromisos.dto.NuevoCompromisoRequest;
import com.pagoflex.compromisos.dto.PagoRequest;
import com.pagoflex.compromisos.dto.PagoResponse;
import com.pagoflex.compromisos.model.CanalPago;
import com.pagoflex.compromisos.model.Comprobante;
import com.pagoflex.compromisos.model.Compromiso;
import com.pagoflex.compromisos.model.Empresa;
import com.pagoflex.compromisos.model.EstadoCompromiso;
import com.pagoflex.compromisos.model.MotivoAnulacion;
import com.pagoflex.compromisos.repository.ComprobanteRepository;
import com.pagoflex.compromisos.repository.CompromisoRepository;
import com.pagoflex.compromisos.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompromisoServiceTest {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final LocalDate HOY = LocalDate.now();
    private static final int ANIO = HOY.getYear();

    @Mock
    private CompromisoRepository compromisoRepository;

    @Mock
    private ComprobanteRepository comprobanteRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private CompromisoService compromisoService;

    @BeforeEach
    void preparar() {
        VencimientoService vencimientoService = new VencimientoService(compromisoRepository, empresaRepository);
        compromisoService = new CompromisoService(compromisoRepository, comprobanteRepository, empresaRepository, vencimientoService);
    }

    private static String fecha(LocalDate dia) {
        return dia.format(FORMATO);
    }

    private Compromiso cuota(String folio, int numero, int total, EstadoCompromiso estado, LocalDate vencimiento) {
        return Compromiso.builder()
                .id(1L)
                .folio(folio)
                .codigoCredito("CR-2026-0002")
                .rutUsuario("15.834.207-3")
                .rutClave("158342073")
                .nombreUsuario("Camila Rojas")
                .empresaCodigo("EC-06")
                .concepto("Cuota social")
                .cuotaActual(numero)
                .totalCuotas(total)
                .monto(15000)
                .fechaVencimiento(fecha(vencimiento))
                .estado(estado)
                .recargoAplicado(0)
                .registradoPor("EJ-05")
                .build();
    }

    private NuevoCompromisoRequest solicitud(String rut, Integer total, Integer inicial, Integer monto, String vencimiento) {
        return new NuevoCompromisoRequest(rut, "Benjamin Araya", "EC-03", "Crédito de consumo",
                total, inicial, monto, vencimiento, "EJ-03");
    }

    private void existe(Compromiso compromiso) {
        when(compromisoRepository.findByFolio(compromiso.getFolio())).thenReturn(Optional.of(compromiso));
    }

    private void guardarDevuelveLoMismo() {
        when(compromisoRepository.save(any(Compromiso.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void recargoDelClub() {
        when(empresaRepository.findById("EC-06")).thenReturn(Optional.of(new Empresa("EC-06", "Club Los Halcones", 2000)));
    }

    private ResponseStatusException esperarError(Runnable accion) {
        return assertThrows(ResponseStatusException.class, accion::run);
    }

    @Test
    void listarPorUsuario_ordenaPorFechaDeVencimiento() {
        when(compromisoRepository.findByRutClave("158342073")).thenReturn(List.of(
                cuota("CP-2026-0201", 3, 12, EstadoCompromiso.PENDIENTE, HOY.plusDays(20)),
                cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY.plusDays(5))
        ));

        List<CompromisoResponse> resultado = compromisoService.listarPorUsuario("15.834.207-3");

        assertEquals("CP-2026-0200", resultado.get(0).folio());
        assertEquals("CP-2026-0201", resultado.get(1).folio());
    }

    @Test
    void listarPorEmpresa_devuelveLosCompromisosDeLaEmpresa() {
        when(compromisoRepository.findByEmpresaCodigo("EC-06"))
                .thenReturn(List.of(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY.plusDays(5))));

        List<CompromisoResponse> resultado = compromisoService.listarPorEmpresa("EC-06");

        assertEquals(1, resultado.size());
        assertEquals("EC-06", resultado.get(0).empresaCodigo());
    }

    @Test
    void obtener_conFolioExistente_devuelveElCompromiso() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY.plusDays(5)));

        CompromisoResponse resultado = compromisoService.obtener("CP-2026-0200");

        assertEquals("Cuota social", resultado.concepto());
    }

    @Test
    void obtener_conFolioInexistente_lanzaNoEncontrado() {
        when(compromisoRepository.findByFolio("CP-0000")).thenReturn(Optional.empty());

        ResponseStatusException error = esperarError(() -> compromisoService.obtener("CP-0000"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Compromiso no encontrado", error.getReason());
    }

    @Test
    void registrar_conCamposVacios_lanzaError() {
        NuevoCompromisoRequest datos = new NuevoCompromisoRequest("", "", "", "", 12, 1, 1000, fecha(HOY), "");

        ResponseStatusException error = esperarError(() -> compromisoService.registrar(datos));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("RUT, nombre, empresa, concepto y ejecutivo son obligatorios", error.getReason());
    }

    @Test
    void registrar_conRutInvalido_lanzaError() {
        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-5", 12, 1, 86400, fecha(HOY))));

        assertEquals("El RUT del deudor no es válido", error.getReason());
    }

    @Test
    void registrar_conEmpresaInexistente_lanzaError() {
        when(empresaRepository.existsById("EC-03")).thenReturn(false);

        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-4", 12, 1, 86400, fecha(HOY))));

        assertEquals("La empresa indicada no existe", error.getReason());
    }

    @Test
    void registrar_conCeroCuotas_lanzaError() {
        when(empresaRepository.existsById("EC-03")).thenReturn(true);

        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-4", 0, 1, 86400, fecha(HOY))));

        assertEquals("La cantidad de cuotas debe estar entre 1 y 120", error.getReason());
    }

    @Test
    void registrar_conCuotaInicialMayorAlTotal_lanzaError() {
        when(empresaRepository.existsById("EC-03")).thenReturn(true);

        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-4", 12, 13, 86400, fecha(HOY))));

        assertEquals("La cuota inicial debe estar entre 1 y 12", error.getReason());
    }

    @Test
    void registrar_conMontoCero_lanzaError() {
        when(empresaRepository.existsById("EC-03")).thenReturn(true);

        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-4", 12, 1, 0, fecha(HOY))));

        assertEquals("El monto de la cuota debe ser mayor a cero", error.getReason());
    }

    @Test
    void registrar_conFechaMalEscrita_lanzaError() {
        when(empresaRepository.existsById("EC-03")).thenReturn(true);

        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-4", 12, 1, 86400, "2026/12/31")));

        assertEquals("La fecha de vencimiento debe tener el formato dd-MM-aaaa", error.getReason());
    }

    @Test
    void registrar_conFechaAnteriorAHoy_lanzaError() {
        when(empresaRepository.existsById("EC-03")).thenReturn(true);

        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-4", 12, 1, 86400, fecha(HOY.minusDays(1)))));

        assertEquals("La fecha de vencimiento no puede ser anterior a hoy", error.getReason());
    }

    @Test
    void registrar_conCuotaYaRegistrada_lanzaConflictoAunqueCambienLasTildes() {
        when(empresaRepository.existsById("EC-03")).thenReturn(true);
        Compromiso anulada = cuota("CP-2026-0119", 1, 12, EstadoCompromiso.ANULADO, HOY);
        anulada.setConcepto("Credito de Consumo");
        Compromiso existente = cuota("CP-2026-0120", 1, 12, EstadoCompromiso.PENDIENTE, HOY);
        existente.setConcepto("Credito de Consumo");
        when(compromisoRepository.findByRutClaveAndEmpresaCodigo("187043964", "EC-03"))
                .thenReturn(List.of(anulada, existente));

        ResponseStatusException error = esperarError(
                () -> compromisoService.registrar(solicitud("18.704.396-4", 12, 1, 86400, fecha(HOY))));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Esta cuota ya está registrada: CP-2026-0120 (cuota 1 de 12)", error.getReason());
    }

    @Test
    void registrar_conDatosCorrectos_creaCuotaPendienteConFolioYCreditoNuevos() {
        when(empresaRepository.existsById("EC-03")).thenReturn(true);
        when(compromisoRepository.findByRutClaveAndEmpresaCodigo("187043964", "EC-03")).thenReturn(List.of());
        Compromiso ultimo = cuota("CP-2026-0113", 11, 12, EstadoCompromiso.PENDIENTE, HOY);
        ultimo.setCodigoCredito("CR-2026-0010");
        Compromiso sinNumero = cuota("SIN-FOLIO", 1, 1, EstadoCompromiso.PAGADO, HOY);
        sinNumero.setCodigoCredito("SIN-CODIGO");
        when(compromisoRepository.findAll()).thenReturn(List.of(ultimo, sinNumero));
        guardarDevuelveLoMismo();

        CompromisoResponse resultado = compromisoService.registrar(
                solicitud(" 18.704.396-4 ", 24, 3, 86400, fecha(HOY.plusDays(30))));

        assertEquals("CP-" + ANIO + "-0114", resultado.folio());
        assertEquals("CR-" + ANIO + "-0011", resultado.codigoCredito());
        assertEquals("18.704.396-4", resultado.rutUsuario());
        assertEquals(3, resultado.cuotaActual());
        assertEquals(24, resultado.totalCuotas());
        assertEquals(EstadoCompromiso.PENDIENTE, resultado.estado());
        assertEquals(0, resultado.recargoAplicado());
    }

    @Test
    void cambiarEstado_sinEstado_lanzaError() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY));

        ResponseStatusException error = esperarError(
                () -> compromisoService.cambiarEstado("CP-2026-0200", new CambiarEstadoRequest(null)));

        assertEquals("Indica el nuevo estado", error.getReason());
    }

    @Test
    void cambiarEstado_aPagado_pideUsarElPago() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.cambiarEstado("CP-2026-0200",
                new CambiarEstadoRequest(EstadoCompromiso.PAGADO)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void cambiarEstado_aAnulado_pideUsarLaAnulacion() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.cambiarEstado("CP-2026-0200",
                new CambiarEstadoRequest(EstadoCompromiso.ANULADO)));

        assertEquals("Para anular un compromiso usa la opción de anular, con su motivo", error.getReason());
    }

    @Test
    void cambiarEstado_dePagadoAEnRevision_lanzaConflicto() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PAGADO, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.cambiarEstado("CP-2026-0200",
                new CambiarEstadoRequest(EstadoCompromiso.EN_REVISION)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("No se puede pasar de PAGADO a EN_REVISION", error.getReason());
    }

    @Test
    void cambiarEstado_dePendienteAEnRevision_loGuarda() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY.plusDays(3)));
        guardarDevuelveLoMismo();

        CompromisoResponse resultado = compromisoService.cambiarEstado("CP-2026-0200",
                new CambiarEstadoRequest(EstadoCompromiso.EN_REVISION));

        assertEquals(EstadoCompromiso.EN_REVISION, resultado.estado());
    }

    @Test
    void cambiarEstado_deRevisionAPendienteConFechaPasada_quedaVencidaConRecargo() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.EN_REVISION, HOY.minusDays(5)));
        guardarDevuelveLoMismo();
        recargoDelClub();

        CompromisoResponse resultado = compromisoService.cambiarEstado("CP-2026-0200",
                new CambiarEstadoRequest(EstadoCompromiso.PENDIENTE));

        assertEquals(EstadoCompromiso.VENCIDO, resultado.estado());
        assertEquals(2000, resultado.recargoAplicado());
    }

    @Test
    void anular_sinMotivo_lanzaError() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.anular("CP-2026-0200",
                new AnularRequest(null, null, "EJ-05", "EC-06")));

        assertEquals("Indica el motivo de la anulación", error.getReason());
    }

    @Test
    void anular_conMotivoOtroSinComentario_lanzaError() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.anular("CP-2026-0200",
                new AnularRequest(MotivoAnulacion.OTRO, " ", "EJ-05", "EC-06")));

        assertEquals("Si el motivo es Otro, escribe un comentario", error.getReason());
    }

    @Test
    void anular_sinEjecutivo_lanzaError() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.anular("CP-2026-0200",
                new AnularRequest(MotivoAnulacion.REPACTACION, null, "", "EC-06")));

        assertEquals("Indica el ejecutivo que anula", error.getReason());
    }

    @Test
    void anular_desdeOtraEmpresa_lanzaProhibido() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PENDIENTE, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.anular("CP-2026-0200",
                new AnularRequest(MotivoAnulacion.REPACTACION, null, "EJ-03", "EC-03")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void anular_compromisoPagado_lanzaConflicto() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PAGADO, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.anular("CP-2026-0200",
                new AnularRequest(MotivoAnulacion.REPACTACION, null, "EJ-05", "EC-06")));

        assertEquals("Un compromiso pagado nunca se anula", error.getReason());
    }

    @Test
    void anular_compromisoYaAnulado_lanzaConflicto() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.ANULADO, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.anular("CP-2026-0200",
                new AnularRequest(MotivoAnulacion.REPACTACION, null, "EJ-05", "EC-06")));

        assertEquals("El compromiso ya está anulado", error.getReason());
    }

    @Test
    void anular_conDatosCorrectos_guardaMotivoEjecutivoYFecha() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.VENCIDO, HOY.minusDays(2)));
        guardarDevuelveLoMismo();

        CompromisoResponse resultado = compromisoService.anular("CP-2026-0200",
                new AnularRequest(MotivoAnulacion.OTRO, " Pagó en caja ", " EJ-05 ", "EC-06"));

        assertEquals(EstadoCompromiso.ANULADO, resultado.estado());
        assertEquals(MotivoAnulacion.OTRO, resultado.motivoAnulacion());
        assertEquals("Pagó en caja", resultado.comentarioAnulacion());
        assertEquals("EJ-05", resultado.anuladoPor());
        assertNotNull(resultado.fechaAnulacion());
    }

    @Test
    void pagar_compromisoYaPagado_lanzaConflicto() {
        existe(cuota("CP-2026-0200", 2, 12, EstadoCompromiso.PAGADO, HOY));

        ResponseStatusException error = esperarError(() -> compromisoService.pagar("CP-2026-0200", null));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Este compromiso no se puede pagar", error.getReason());
    }

    @Test
    void pagar_cuotaVencida_cobraElRecargoYGeneraLaSiguienteCuota() {
        LocalDate vencimiento = HOY.minusDays(10);
        Compromiso vencida = cuota("CP-2026-0102", 10, 12, EstadoCompromiso.VENCIDO, vencimiento);
        vencida.setRecargoAplicado(2000);
        existe(vencida);
        guardarDevuelveLoMismo();
        when(comprobanteRepository.findAll()).thenReturn(List.of(Comprobante.builder().numero("CPR-000453").build()));
        when(comprobanteRepository.save(any(Comprobante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(compromisoRepository.findByCodigoCreditoAndCuotaActual("CR-2026-0002", 11)).thenReturn(Optional.empty());
        when(compromisoRepository.findByRutClaveAndEmpresaCodigo("158342073", "EC-06")).thenReturn(List.of(vencida));
        when(compromisoRepository.findAll()).thenReturn(List.of(vencida));

        PagoResponse resultado = compromisoService.pagar("CP-2026-0102", null);

        ComprobanteResponse comprobante = resultado.comprobante();
        assertEquals("CPR-000454", comprobante.numero());
        assertEquals(17000, comprobante.montoPagado());
        assertFalse(comprobante.aTiempo());
        assertEquals(CanalPago.APP, comprobante.canal());
        assertEquals("Pago simulado", comprobante.medioPago());
        assertEquals(EstadoCompromiso.PAGADO, vencida.getEstado());
        CompromisoResponse siguiente = resultado.siguienteCuota();
        assertEquals(11, siguiente.cuotaActual());
        assertEquals(fecha(vencimiento.plusMonths(1)), siguiente.fechaVencimiento());
        assertEquals(EstadoCompromiso.PENDIENTE, siguiente.estado());
        assertEquals("CR-2026-0002", siguiente.codigoCredito());
    }

    @Test
    void pagar_siguienteCuotaTambienAtrasada_laDejaVencidaConRecargo() {
        Compromiso atrasada = cuota("CP-2026-0102", 5, 12, EstadoCompromiso.PENDIENTE, HOY.minusMonths(2));
        existe(atrasada);
        when(compromisoRepository.findByEstado(EstadoCompromiso.PENDIENTE)).thenReturn(List.of(atrasada));
        recargoDelClub();
        guardarDevuelveLoMismo();
        when(comprobanteRepository.findAll()).thenReturn(List.of());
        when(comprobanteRepository.save(any(Comprobante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(compromisoRepository.findByCodigoCreditoAndCuotaActual("CR-2026-0002", 6)).thenReturn(Optional.empty());
        when(compromisoRepository.findByRutClaveAndEmpresaCodigo("158342073", "EC-06")).thenReturn(List.of(atrasada));
        when(compromisoRepository.findAll()).thenReturn(List.of(atrasada));

        PagoResponse resultado = compromisoService.pagar("CP-2026-0102", null);

        assertEquals("CPR-000001", resultado.comprobante().numero());
        assertEquals(17000, resultado.comprobante().montoPagado());
        assertEquals(EstadoCompromiso.VENCIDO, resultado.siguienteCuota().estado());
        assertEquals(2000, resultado.siguienteCuota().recargoAplicado());
    }

    @Test
    void pagar_ultimaCuotaATiempo_noGeneraOtraCuota() {
        existe(cuota("CP-2026-0114", 12, 12, EstadoCompromiso.PENDIENTE, HOY.plusDays(5)));
        guardarDevuelveLoMismo();
        when(comprobanteRepository.findAll()).thenReturn(List.of());
        when(comprobanteRepository.save(any(Comprobante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PagoResponse resultado = compromisoService.pagar("CP-2026-0114",
                new PagoRequest(CanalPago.PORTAL, " Tarjeta de débito "));

        assertTrue(resultado.comprobante().aTiempo());
        assertEquals(15000, resultado.comprobante().montoPagado());
        assertEquals(CanalPago.PORTAL, resultado.comprobante().canal());
        assertEquals("Tarjeta de débito", resultado.comprobante().medioPago());
        assertNull(resultado.siguienteCuota());
    }

    @Test
    void pagar_conSiguienteCuotaYaRegistrada_noCreaOtra() {
        existe(cuota("CP-2026-0102", 10, 12, EstadoCompromiso.PENDIENTE, HOY.plusDays(2)));
        guardarDevuelveLoMismo();
        when(comprobanteRepository.findAll()).thenReturn(List.of());
        when(comprobanteRepository.save(any(Comprobante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        Compromiso yaRegistrada = cuota("CP-2026-0150", 11, 12, EstadoCompromiso.PENDIENTE, HOY.plusMonths(1));
        when(compromisoRepository.findByCodigoCreditoAndCuotaActual("CR-2026-0002", 11)).thenReturn(Optional.of(yaRegistrada));

        PagoResponse resultado = compromisoService.pagar("CP-2026-0102", null);

        assertEquals("CP-2026-0150", resultado.siguienteCuota().folio());
        verify(compromisoRepository, times(1)).save(any(Compromiso.class));
    }

    @Test
    void actualizarNombreTitular_sinNombre_lanzaError() {
        ResponseStatusException error = esperarError(
                () -> compromisoService.actualizarNombreTitular("15.834.207-3", new NombreTitularRequest(" ")));

        assertEquals("Indica el nombre", error.getReason());
    }

    @Test
    @SuppressWarnings("unchecked")
    void actualizarNombreTitular_cambiaElNombreEnTodosSusCompromisos() {
        List<Compromiso> deCamila = List.of(
                cuota("CP-2026-0101", 10, 12, EstadoCompromiso.PENDIENTE, HOY),
                cuota("CP-2026-0102", 10, 12, EstadoCompromiso.VENCIDO, HOY));
        when(compromisoRepository.findByRutClave("158342073")).thenReturn(deCamila);

        compromisoService.actualizarNombreTitular("15.834.207-3", new NombreTitularRequest(" Camila Andrea Rojas "));

        ArgumentCaptor<List<Compromiso>> guardados = ArgumentCaptor.forClass(List.class);
        verify(compromisoRepository).saveAll(guardados.capture());
        assertTrue(guardados.getValue().stream().allMatch(c -> c.getNombreUsuario().equals("Camila Andrea Rojas")));
    }

    @Test
    void listarComprobantes_devuelveLosDelUsuario() {
        Comprobante comprobante = Comprobante.builder()
                .numero("CPR-000402")
                .compromisoFolio("CP-2026-0090")
                .rutUsuario("15.834.207-3")
                .empresaCodigo("EC-06")
                .montoPagado(17000)
                .canal(CanalPago.PORTAL)
                .aTiempo(false)
                .build();
        when(comprobanteRepository.findByRutClaveOrderByNumeroDesc("158342073")).thenReturn(List.of(comprobante));

        List<ComprobanteResponse> resultado = compromisoService.listarComprobantes("15.834.207-3");

        assertEquals(1, resultado.size());
        assertEquals("CPR-000402", resultado.get(0).numero());
        assertEquals(17000, resultado.get(0).montoPagado());
    }
}
