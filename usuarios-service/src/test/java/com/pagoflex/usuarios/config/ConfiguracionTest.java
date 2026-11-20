package com.pagoflex.usuarios.config;

import com.pagoflex.usuarios.model.RolId;
import com.pagoflex.usuarios.model.Usuario;
import com.pagoflex.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @SuppressWarnings("unchecked")
    void datosIniciales_conTablaVacia_guardaLasVeinteCuentas() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("Pagoflex1")).thenReturn("clave-encriptada");
        ArgumentCaptor<List<Usuario>> guardados = ArgumentCaptor.forClass(List.class);

        new DatosIniciales(usuarioRepository, passwordEncoder).run();

        verify(usuarioRepository).saveAll(guardados.capture());
        List<Usuario> cuentas = guardados.getValue();
        assertEquals(20, cuentas.size());
        assertEquals(8, cuentas.stream().filter(u -> u.getRolId().equals(RolId.USUARIO_FINAL)).count());
        assertEquals("clave-encriptada", cuentas.get(0).getContrasena());
    }

    @Test
    void datosIniciales_conCuentasGuardadas_noGuardaNada() {
        when(usuarioRepository.count()).thenReturn(20L);

        new DatosIniciales(usuarioRepository, passwordEncoder).run();

        verify(usuarioRepository, never()).saveAll(any());
    }

    @Test
    void passwordEncoder_encriptaYReconoceLaClave() {
        PasswordEncoder encriptador = new SeguridadConfig().passwordEncoder();

        String encriptada = encriptador.encode("Pagoflex1");

        assertNotEquals("Pagoflex1", encriptada);
        assertTrue(encriptador.matches("Pagoflex1", encriptada));
        assertFalse(encriptador.matches("Otra1234", encriptada));
    }

    @Test
    void webClientBuilder_creaUnConstructor() {
        assertNotNull(new WebClientConfig().webClientBuilder());
    }

    @Test
    void documentacion_tieneElNombreDelServicio() {
        assertEquals("PagoFlex - usuarios-service", new SwaggerConfig().documentacion().getInfo().getTitle());
    }
}
