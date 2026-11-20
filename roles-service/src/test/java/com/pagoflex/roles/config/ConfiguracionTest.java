package com.pagoflex.roles.config;

import com.pagoflex.roles.model.Rol;
import com.pagoflex.roles.repository.RolRepository;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private DatosIniciales datosIniciales;

    @Test
    @SuppressWarnings("unchecked")
    void run_conTablaVacia_guardaLosTresRoles() {
        when(rolRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Rol>> guardados = ArgumentCaptor.forClass(List.class);

        datosIniciales.run();

        verify(rolRepository).saveAll(guardados.capture());
        assertEquals(3, guardados.getValue().size());
        assertEquals("Usuario final", guardados.getValue().get(0).getNombre());
    }

    @Test
    void run_conRolesYaGuardados_noGuardaNada() {
        when(rolRepository.count()).thenReturn(3L);

        datosIniciales.run();

        verify(rolRepository, never()).saveAll(any());
    }

    @Test
    void documentacion_tieneElNombreDelServicio() {
        OpenAPI documentacion = new SwaggerConfig().documentacion();

        assertEquals("PagoFlex - roles-service", documentacion.getInfo().getTitle());
    }
}
