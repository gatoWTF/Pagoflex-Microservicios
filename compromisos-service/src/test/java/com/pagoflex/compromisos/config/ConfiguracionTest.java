package com.pagoflex.compromisos.config;

import com.pagoflex.compromisos.model.Compromiso;
import com.pagoflex.compromisos.model.EstadoCompromiso;
import com.pagoflex.compromisos.repository.ComprobanteRepository;
import com.pagoflex.compromisos.repository.CompromisoRepository;
import com.pagoflex.compromisos.repository.EmpresaRepository;
import com.pagoflex.compromisos.repository.ReporteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionTest {

    @Mock
    private CompromisoRepository compromisoRepository;

    @Mock
    private ComprobanteRepository comprobanteRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private ReporteRepository reporteRepository;

    @InjectMocks
    private DatosIniciales datosIniciales;

    @Test
    void datosIniciales_conBaseVacia_cargaTodaLaSemilla() {
        when(empresaRepository.count()).thenReturn(0L);
        when(compromisoRepository.count()).thenReturn(0L);
        when(comprobanteRepository.count()).thenReturn(0L);
        when(reporteRepository.count()).thenReturn(0L);
        when(compromisoRepository.findByFolio(anyString())).thenReturn(Optional.empty());
        Compromiso deLuis = Compromiso.builder().folio("CP-2026-0106").estado(EstadoCompromiso.VENCIDO).build();
        when(compromisoRepository.findByFolio("CP-2026-0106")).thenReturn(Optional.of(deLuis));

        datosIniciales.run();

        verify(empresaRepository).saveAll(anyList());
        verify(compromisoRepository).saveAll(anyList());
        verify(comprobanteRepository).saveAll(anyList());
        verify(reporteRepository).saveAll(anyList());
        verify(compromisoRepository, times(6)).save(any(Compromiso.class));
        assertEquals(EstadoCompromiso.EN_REVISION, deLuis.getEstado());
    }

    @Test
    void datosIniciales_conDatosYaCargados_noRepiteLaSemilla() {
        when(empresaRepository.count()).thenReturn(8L);
        when(compromisoRepository.count()).thenReturn(13L);
        when(comprobanteRepository.count()).thenReturn(8L);
        when(reporteRepository.count()).thenReturn(6L);
        when(compromisoRepository.findByFolio(anyString()))
                .thenReturn(Optional.of(Compromiso.builder().estado(EstadoCompromiso.PAGADO).build()));

        datosIniciales.run();

        verify(compromisoRepository, never()).saveAll(anyList());
        verify(compromisoRepository, never()).save(any(Compromiso.class));
    }

    @Test
    void documentacion_tieneElNombreDelServicio() {
        assertEquals("PagoFlex - compromisos-service", new SwaggerConfig().documentacion().getInfo().getTitle());
    }
}
