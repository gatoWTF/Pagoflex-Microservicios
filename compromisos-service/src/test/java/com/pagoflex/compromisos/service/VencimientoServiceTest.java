package com.pagoflex.compromisos.service;

import com.pagoflex.compromisos.model.Compromiso;
import com.pagoflex.compromisos.model.Empresa;
import com.pagoflex.compromisos.model.EstadoCompromiso;
import com.pagoflex.compromisos.repository.CompromisoRepository;
import com.pagoflex.compromisos.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VencimientoServiceTest {

    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private CompromisoRepository compromisoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private VencimientoService vencimientoService;

    private Compromiso cuota(String folio, LocalDate vencimiento, int recargo) {
        return Compromiso.builder()
                .folio(folio)
                .empresaCodigo("EC-01")
                .fechaVencimiento(vencimiento.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .estado(EstadoCompromiso.PENDIENTE)
                .recargoAplicado(recargo)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void actualizarVencidos_marcaSoloLasCuotasAtrasadas() {
        Compromiso atrasada = cuota("CP-2026-0106", HOY.minusDays(1), 0);
        Compromiso alDia = cuota("CP-2026-0107", HOY, 0);
        when(compromisoRepository.findByEstado(EstadoCompromiso.PENDIENTE)).thenReturn(List.of(atrasada, alDia));
        when(empresaRepository.findById("EC-01")).thenReturn(Optional.of(new Empresa("EC-01", "Altos del Maipo", 8500)));

        vencimientoService.actualizarVencidos();

        assertEquals(EstadoCompromiso.VENCIDO, atrasada.getEstado());
        assertEquals(8500, atrasada.getRecargoAplicado());
        assertEquals(EstadoCompromiso.PENDIENTE, alDia.getEstado());
        ArgumentCaptor<List<Compromiso>> guardados = ArgumentCaptor.forClass(List.class);
        verify(compromisoRepository).saveAll(guardados.capture());
        assertEquals(List.of(atrasada), guardados.getValue());
    }

    @Test
    void estaAtrasado_comparaContraLaFechaIndicada() {
        Compromiso cuota = cuota("CP-2026-0106", HOY, 0);

        assertFalse(vencimientoService.estaAtrasado(cuota, HOY));
        assertTrue(vencimientoService.estaAtrasado(cuota, HOY.plusDays(1)));
    }

    @Test
    void marcarVencido_conRecargoYaAplicado_noLoSumaOtraVez() {
        Compromiso cuota = cuota("CP-2026-0106", HOY.minusDays(3), 8500);

        vencimientoService.marcarVencido(cuota);

        assertEquals(EstadoCompromiso.VENCIDO, cuota.getEstado());
        assertEquals(8500, cuota.getRecargoAplicado());
        verify(empresaRepository, never()).findById(any());
    }

    @Test
    void marcarVencido_conEmpresaDesconocida_dejaRecargoEnCero() {
        Compromiso cuota = cuota("CP-2026-0106", HOY.minusDays(3), 0);
        when(empresaRepository.findById("EC-01")).thenReturn(Optional.empty());

        vencimientoService.marcarVencido(cuota);

        assertEquals(EstadoCompromiso.VENCIDO, cuota.getEstado());
        assertEquals(0, cuota.getRecargoAplicado());
    }
}
