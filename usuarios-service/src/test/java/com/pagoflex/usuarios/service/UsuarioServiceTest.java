package com.pagoflex.usuarios.service;

import com.pagoflex.usuarios.client.CompromisoClient;
import com.pagoflex.usuarios.client.RolClient;
import com.pagoflex.usuarios.dto.ActualizarUsuarioRequest;
import com.pagoflex.usuarios.dto.CambiarContrasenaRequest;
import com.pagoflex.usuarios.dto.ContactoRequest;
import com.pagoflex.usuarios.dto.LoginRequest;
import com.pagoflex.usuarios.dto.RegistroRequest;
import com.pagoflex.usuarios.dto.RolDto;
import com.pagoflex.usuarios.dto.TitularResponse;
import com.pagoflex.usuarios.dto.UsuarioResponse;
import com.pagoflex.usuarios.model.RolId;
import com.pagoflex.usuarios.model.Usuario;
import com.pagoflex.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String CLAVE_ENCRIPTADA = "clave-encriptada";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolClient rolClient;

    @Mock
    private CompromisoClient compromisoClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario camila() {
        return Usuario.builder()
                .id(1L)
                .nombre("Camila Rojas")
                .rut("15.834.207-3")
                .correo("crojas@correo.ejemplo")
                .contrasena(CLAVE_ENCRIPTADA)
                .rolId(RolId.USUARIO_FINAL)
                .build();
    }

    private void rolUsuarioFinalDisponible() {
        when(rolClient.obtener(RolId.USUARIO_FINAL)).thenReturn(new RolDto(1L, "Usuario final"));
    }

    private void guardarDevuelveLoMismo() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private ResponseStatusException esperarError(Runnable accion) {
        return assertThrows(ResponseStatusException.class, accion::run);
    }

    @Test
    void obtener_conIdExistente_devuelveDatosConNombreDelRol() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        rolUsuarioFinalDisponible();

        UsuarioResponse resultado = usuarioService.obtener(1L);

        assertEquals("Camila Rojas", resultado.nombre());
        assertEquals("Usuario final", resultado.rolNombre());
    }

    @Test
    void obtener_conRolQueNoExiste_devuelveNombreDeRolVacio() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(rolClient.obtener(RolId.USUARIO_FINAL)).thenReturn(null);

        UsuarioResponse resultado = usuarioService.obtener(1L);

        assertEquals("", resultado.rolNombre());
    }

    @Test
    void obtener_conIdInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException error = esperarError(() -> usuarioService.obtener(99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Usuario no encontrado", error.getReason());
    }

    @Test
    void buscarTitular_sinRut_lanzaError() {
        ResponseStatusException error = esperarError(() -> usuarioService.buscarTitular(" "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Indica el RUT", error.getReason());
    }

    @Test
    void buscarTitular_conRutSinPuntos_encuentraAlTitular() {
        when(usuarioRepository.findAll()).thenReturn(List.of(camila()));

        TitularResponse resultado = usuarioService.buscarTitular("158342073");

        assertEquals("15.834.207-3", resultado.rut());
        assertEquals("Camila Rojas", resultado.nombre());
    }

    @Test
    void buscarTitular_conRutSinCuenta_lanzaNoEncontrado() {
        when(usuarioRepository.findAll()).thenReturn(List.of(camila()));

        ResponseStatusException error = esperarError(() -> usuarioService.buscarTitular("11.111.111-1"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void registrar_conCamposVacios_lanzaError() {
        RegistroRequest datos = new RegistroRequest("", "", "", "");

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Nombre, RUT y correo son obligatorios", error.getReason());
    }

    @Test
    void registrar_conNombreConNumeros_lanzaError() {
        RegistroRequest datos = new RegistroRequest("Ana 123", "11.111.111-1", "ana@correo.ejemplo", "Clave123");

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals("El nombre no puede llevar números", error.getReason());
    }

    @Test
    void registrar_conCorreoSinDominio_lanzaError() {
        RegistroRequest datos = new RegistroRequest("Ana Perez", "11.111.111-1", "ana@correo", "Clave123");

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals("Correo no válido", error.getReason());
    }

    @Test
    void registrar_conDigitoVerificadorIncorrecto_lanzaError() {
        RegistroRequest datos = new RegistroRequest("Ana Perez", "11.111.111-2", "ana@correo.ejemplo", "Clave123");

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals("RUT no válido", error.getReason());
    }

    @Test
    void registrar_conContrasenaSinMayuscula_lanzaError() {
        RegistroRequest datos = new RegistroRequest("Ana Perez", "11.111.111-1", "ana@correo.ejemplo", "clave123");

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().startsWith("La contraseña debe tener al menos 6 caracteres"));
    }

    @Test
    void registrar_conCorreoRepetido_lanzaConflicto() {
        RegistroRequest datos = new RegistroRequest("Ana Perez", "11.111.111-1", "crojas@correo.ejemplo", "Clave123");
        when(usuarioRepository.existsByCorreo("crojas@correo.ejemplo")).thenReturn(true);

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Ya existe una cuenta con este correo", error.getReason());
    }

    @Test
    void registrar_conRutRepetido_lanzaConflicto() {
        RegistroRequest datos = new RegistroRequest("Ana Perez", "15834207-3", "ana@correo.ejemplo", "Clave123");
        when(usuarioRepository.existsByCorreo("ana@correo.ejemplo")).thenReturn(false);
        when(usuarioRepository.findAll()).thenReturn(List.of(camila()));

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Ya existe una cuenta con este RUT", error.getReason());
    }

    @Test
    void registrar_conRolInexistente_lanzaError() {
        RegistroRequest datos = new RegistroRequest("Ana Perez", "11.111.111-1", "ana@correo.ejemplo", "Clave123");
        when(usuarioRepository.existsByCorreo("ana@correo.ejemplo")).thenReturn(false);
        when(usuarioRepository.findAll()).thenReturn(List.of());
        when(rolClient.obtener(RolId.USUARIO_FINAL)).thenReturn(null);

        ResponseStatusException error = esperarError(() -> usuarioService.registrar(datos));

        assertEquals("El rol indicado no existe", error.getReason());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_conDatosCorrectos_guardaUsuarioFinalConClaveEncriptada() {
        RegistroRequest datos = new RegistroRequest(" Ana Perez ", "11.111.111-1", " ANA@Correo.Ejemplo ", "Clave123");
        when(usuarioRepository.existsByCorreo("ana@correo.ejemplo")).thenReturn(false);
        when(usuarioRepository.findAll()).thenReturn(List.of(camila()));
        rolUsuarioFinalDisponible();
        when(passwordEncoder.encode("Clave123")).thenReturn(CLAVE_ENCRIPTADA);
        guardarDevuelveLoMismo();

        UsuarioResponse resultado = usuarioService.registrar(datos);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertEquals(CLAVE_ENCRIPTADA, guardado.getValue().getContrasena());
        assertEquals(RolId.USUARIO_FINAL, guardado.getValue().getRolId());
        assertEquals("Ana Perez", resultado.nombre());
        assertEquals("ana@correo.ejemplo", resultado.correo());
        assertEquals("Usuario final", resultado.rolNombre());
    }

    @Test
    void iniciarSesion_conCamposVacios_lanzaError() {
        ResponseStatusException error = esperarError(() -> usuarioService.iniciarSesion(new LoginRequest("", "")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Ingresa tu correo y tu contraseña", error.getReason());
    }

    @Test
    void iniciarSesion_conContrasenaIncorrecta_lanzaMensajeGenerico() {
        when(usuarioRepository.findByCorreo("crojas@correo.ejemplo")).thenReturn(Optional.of(camila()));
        when(passwordEncoder.matches("Mala123", CLAVE_ENCRIPTADA)).thenReturn(false);

        ResponseStatusException error = esperarError(
                () -> usuarioService.iniciarSesion(new LoginRequest("crojas@correo.ejemplo", "Mala123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("Correo y/o contraseña incorrectos", error.getReason());
    }

    @Test
    void iniciarSesion_conCorreoInexistente_lanzaElMismoMensaje() {
        when(usuarioRepository.findByCorreo("nadie@correo.ejemplo")).thenReturn(Optional.empty());

        ResponseStatusException error = esperarError(
                () -> usuarioService.iniciarSesion(new LoginRequest("nadie@correo.ejemplo", "Clave123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("Correo y/o contraseña incorrectos", error.getReason());
    }

    @Test
    void iniciarSesion_conDatosCorrectos_devuelveLaCuenta() {
        when(usuarioRepository.findByCorreo("crojas@correo.ejemplo")).thenReturn(Optional.of(camila()));
        when(passwordEncoder.matches("Pagoflex1", CLAVE_ENCRIPTADA)).thenReturn(true);
        rolUsuarioFinalDisponible();

        UsuarioResponse resultado = usuarioService.iniciarSesion(new LoginRequest(" CRojas@correo.ejemplo ", "Pagoflex1"));

        assertEquals(1L, resultado.id());
        assertEquals("Usuario final", resultado.rolNombre());
    }

    @Test
    void actualizar_conCamposVacios_lanzaError() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));

        ResponseStatusException error = esperarError(
                () -> usuarioService.actualizar(1L, new ActualizarUsuarioRequest("", "", "")));

        assertEquals("Nombre, RUT y correo son obligatorios", error.getReason());
    }

    @Test
    void actualizar_conOtroRut_lanzaError() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        ActualizarUsuarioRequest datos = new ActualizarUsuarioRequest("Camila Rojas", "11.111.111-1", "crojas@correo.ejemplo");

        ResponseStatusException error = esperarError(() -> usuarioService.actualizar(1L, datos));

        assertEquals("El RUT no se puede cambiar", error.getReason());
    }

    @Test
    void actualizar_conCorreoDeOtraCuenta_lanzaConflicto() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(usuarioRepository.existsByCorreoAndIdNot("otro@correo.ejemplo", 1L)).thenReturn(true);
        ActualizarUsuarioRequest datos = new ActualizarUsuarioRequest("Camila Rojas", "15.834.207-3", "otro@correo.ejemplo");

        ResponseStatusException error = esperarError(() -> usuarioService.actualizar(1L, datos));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void actualizar_conDatosCorrectos_guardaYAvisaACompromisos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(usuarioRepository.existsByCorreoAndIdNot("camila@correo.ejemplo", 1L)).thenReturn(false);
        guardarDevuelveLoMismo();
        rolUsuarioFinalDisponible();
        ActualizarUsuarioRequest datos = new ActualizarUsuarioRequest(" Camila Andrea Rojas ", "158342073", "Camila@correo.ejemplo");

        UsuarioResponse resultado = usuarioService.actualizar(1L, datos);

        assertEquals("Camila Andrea Rojas", resultado.nombre());
        assertEquals("camila@correo.ejemplo", resultado.correo());
        verify(compromisoClient).actualizarNombreTitular("15.834.207-3", "Camila Andrea Rojas");
    }

    @Test
    void guardarContacto_conCorreoSecundarioInvalido_lanzaError() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));

        ResponseStatusException error = esperarError(
                () -> usuarioService.guardarContacto(1L, new ContactoRequest("no-es-correo", null)));

        assertEquals("Correo secundario no válido", error.getReason());
    }

    @Test
    void guardarContacto_conTelefonoConLetras_lanzaError() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));

        ResponseStatusException error = esperarError(
                () -> usuarioService.guardarContacto(1L, new ContactoRequest(null, "12ab")));

        assertEquals("Teléfono no válido", error.getReason());
    }

    @Test
    void guardarContacto_conDatosCorrectos_guardaCorreoYTelefono() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        guardarDevuelveLoMismo();
        rolUsuarioFinalDisponible();

        UsuarioResponse resultado = usuarioService.guardarContacto(1L,
                new ContactoRequest(" Camila@Gmail.com ", " +56 9 1234 5678 "));

        assertEquals("camila@gmail.com", resultado.correoSecundario());
        assertEquals("+56 9 1234 5678", resultado.telefono());
    }

    @Test
    void guardarContacto_sinDatos_borraElContacto() {
        Usuario usuario = camila();
        usuario.setTelefono("+56912345678");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        guardarDevuelveLoMismo();
        rolUsuarioFinalDisponible();

        UsuarioResponse resultado = usuarioService.guardarContacto(1L, null);

        assertNull(resultado.correoSecundario());
        assertNull(resultado.telefono());
    }

    @Test
    void cambiarContrasena_conActualIncorrecta_lanzaNoAutorizado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(passwordEncoder.matches("Mala123", CLAVE_ENCRIPTADA)).thenReturn(false);

        ResponseStatusException error = esperarError(
                () -> usuarioService.cambiarContrasena(1L, new CambiarContrasenaRequest("Mala123", "Nueva2026")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("La contraseña actual es incorrecta", error.getReason());
    }

    @Test
    void cambiarContrasena_conNuevaMuyCorta_lanzaError() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(passwordEncoder.matches("Pagoflex1", CLAVE_ENCRIPTADA)).thenReturn(true);

        ResponseStatusException error = esperarError(
                () -> usuarioService.cambiarContrasena(1L, new CambiarContrasenaRequest("Pagoflex1", "Ab1")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void cambiarContrasena_conNuevaIgualALaActual_lanzaError() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(passwordEncoder.matches("Pagoflex1", CLAVE_ENCRIPTADA)).thenReturn(true);

        ResponseStatusException error = esperarError(
                () -> usuarioService.cambiarContrasena(1L, new CambiarContrasenaRequest("Pagoflex1", "Pagoflex1")));

        assertEquals("La nueva contraseña debe ser distinta a la actual", error.getReason());
    }

    @Test
    void cambiarContrasena_conDatosCorrectos_guardaLaNuevaEncriptada() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(passwordEncoder.matches("Pagoflex1", CLAVE_ENCRIPTADA)).thenReturn(true);
        when(passwordEncoder.matches("Nueva2026", CLAVE_ENCRIPTADA)).thenReturn(false);
        when(passwordEncoder.encode("Nueva2026")).thenReturn("nueva-encriptada");

        usuarioService.cambiarContrasena(1L, new CambiarContrasenaRequest("Pagoflex1", "Nueva2026"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertEquals("nueva-encriptada", guardado.getValue().getContrasena());
    }

    @Test
    void eliminar_conCuentaInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseStatusException error = esperarError(() -> usuarioService.eliminar(9L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void eliminar_cuentaDeEjecutivo_lanzaProhibido() {
        Usuario ejecutiva = camila();
        ejecutiva.setRolId(RolId.EJECUTIVO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ejecutiva));

        ResponseStatusException error = esperarError(() -> usuarioService.eliminar(1L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void eliminar_conCuotasPorPagar_lanzaConflicto() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(camila()));
        when(compromisoClient.tieneCuotasPorPagar("15.834.207-3")).thenReturn(true);

        ResponseStatusException error = esperarError(() -> usuarioService.eliminar(1L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("No puedes eliminar tu cuenta mientras tengas cuotas por pagar", error.getReason());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void eliminar_sinCuotasPorPagar_borraLaCuenta() {
        Usuario cuenta = camila();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        when(compromisoClient.tieneCuotasPorPagar("15.834.207-3")).thenReturn(false);

        usuarioService.eliminar(1L);

        verify(usuarioRepository).delete(cuenta);
    }
}
