package com.pagoflex.roles.controller;

import com.pagoflex.roles.model.Rol;
import com.pagoflex.roles.service.RolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    @Mock
    private RolService rolService;

    @InjectMocks
    private RolController rolController;

    @Test
    void listar_devuelveLoQueEntregaElServicio() {
        when(rolService.listar()).thenReturn(List.of(new Rol(1L, "Usuario final")));

        List<Rol> resultado = rolController.listar();

        assertEquals(1, resultado.size());
        assertEquals("Usuario final", resultado.get(0).getNombre());
    }

    @Test
    void obtener_conId_devuelveElRolDelServicio() {
        when(rolService.obtener(3L)).thenReturn(new Rol(3L, "Agente PagoFlex"));

        Rol resultado = rolController.obtener(3L);

        assertEquals(3L, resultado.getId());
        assertEquals("Agente PagoFlex", resultado.getNombre());
    }
}
