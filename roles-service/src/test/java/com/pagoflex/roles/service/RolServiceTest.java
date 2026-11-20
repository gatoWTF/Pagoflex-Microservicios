package com.pagoflex.roles.service;

import com.pagoflex.roles.model.Rol;
import com.pagoflex.roles.repository.RolRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolService rolService;

    @Test
    void listar_conTresRoles_devuelveLosTres() {
        when(rolRepository.findAll()).thenReturn(List.of(
                new Rol(1L, "Usuario final"),
                new Rol(2L, "Ejecutivo de empresa"),
                new Rol(3L, "Agente PagoFlex")
        ));

        List<Rol> resultado = rolService.listar();

        assertEquals(3, resultado.size());
        assertEquals("Agente PagoFlex", resultado.get(2).getNombre());
    }

    @Test
    void obtener_conIdExistente_devuelveElRol() {
        when(rolRepository.findById(2L)).thenReturn(Optional.of(new Rol(2L, "Ejecutivo de empresa")));

        Rol resultado = rolService.obtener(2L);

        assertEquals("Ejecutivo de empresa", resultado.getNombre());
    }

    @Test
    void obtener_conIdInexistente_lanzaNoEncontrado() {
        when(rolRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> rolService.obtener(9L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Rol no encontrado", error.getReason());
    }
}
