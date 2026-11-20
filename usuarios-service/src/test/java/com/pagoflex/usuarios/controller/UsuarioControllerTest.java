package com.pagoflex.usuarios.controller;

import com.pagoflex.usuarios.dto.ActualizarUsuarioRequest;
import com.pagoflex.usuarios.dto.CambiarContrasenaRequest;
import com.pagoflex.usuarios.dto.ContactoRequest;
import com.pagoflex.usuarios.dto.LoginRequest;
import com.pagoflex.usuarios.dto.RegistroRequest;
import com.pagoflex.usuarios.dto.TitularResponse;
import com.pagoflex.usuarios.dto.UsuarioResponse;
import com.pagoflex.usuarios.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private final UsuarioResponse camila = new UsuarioResponse(1L, "Camila Rojas", "15.834.207-3",
            "crojas@correo.ejemplo", 1L, "Usuario final", null, null);

    @Test
    void obtener_devuelveLaCuentaDelServicio() {
        when(usuarioService.obtener(1L)).thenReturn(camila);

        assertEquals(camila, usuarioController.obtener(1L));
    }

    @Test
    void buscarTitular_devuelveElTitularDelServicio() {
        TitularResponse titular = new TitularResponse("15.834.207-3", "Camila Rojas");
        when(usuarioService.buscarTitular("15.834.207-3")).thenReturn(titular);

        assertEquals(titular, usuarioController.buscarTitular("15.834.207-3"));
    }

    @Test
    void registrar_entregaLosDatosAlServicio() {
        RegistroRequest datos = new RegistroRequest("Camila Rojas", "15.834.207-3", "crojas@correo.ejemplo", "Pagoflex1");
        when(usuarioService.registrar(datos)).thenReturn(camila);

        assertEquals(camila, usuarioController.registrar(datos));
    }

    @Test
    void iniciarSesion_entregaLosDatosAlServicio() {
        LoginRequest datos = new LoginRequest("crojas@correo.ejemplo", "Pagoflex1");
        when(usuarioService.iniciarSesion(datos)).thenReturn(camila);

        assertEquals(camila, usuarioController.iniciarSesion(datos));
    }

    @Test
    void actualizar_entregaLosDatosAlServicio() {
        ActualizarUsuarioRequest datos = new ActualizarUsuarioRequest("Camila Rojas", "15.834.207-3", "crojas@correo.ejemplo");
        when(usuarioService.actualizar(1L, datos)).thenReturn(camila);

        assertEquals(camila, usuarioController.actualizar(1L, datos));
    }

    @Test
    void guardarContacto_entregaLosDatosAlServicio() {
        ContactoRequest datos = new ContactoRequest("camila@gmail.com", "+56912345678");
        when(usuarioService.guardarContacto(1L, datos)).thenReturn(camila);

        assertEquals(camila, usuarioController.guardarContacto(1L, datos));
    }

    @Test
    void cambiarContrasena_llamaAlServicio() {
        CambiarContrasenaRequest datos = new CambiarContrasenaRequest("Pagoflex1", "Nueva2026");

        usuarioController.cambiarContrasena(1L, datos);

        verify(usuarioService).cambiarContrasena(1L, datos);
    }
}
