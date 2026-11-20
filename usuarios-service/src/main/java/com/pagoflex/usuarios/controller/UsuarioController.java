package com.pagoflex.usuarios.controller;

import com.pagoflex.usuarios.dto.ActualizarUsuarioRequest;
import com.pagoflex.usuarios.dto.CambiarContrasenaRequest;
import com.pagoflex.usuarios.dto.ContactoRequest;
import com.pagoflex.usuarios.dto.LoginRequest;
import com.pagoflex.usuarios.dto.RegistroRequest;
import com.pagoflex.usuarios.dto.TitularResponse;
import com.pagoflex.usuarios.dto.UsuarioResponse;
import com.pagoflex.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Cuentas, inicio de sesión y perfil")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una cuenta por su id")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @GetMapping("/titular")
    @Operation(summary = "Busca el titular de un RUT")
    public TitularResponse buscarTitular(@RequestParam String rut) {
        return usuarioService.buscarTitular(rut);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una cuenta nueva de usuario final")
    public UsuarioResponse registrar(@RequestBody RegistroRequest datos) {
        return usuarioService.registrar(datos);
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión con correo y contraseña")
    public UsuarioResponse iniciarSesion(@RequestBody LoginRequest datos) {
        return usuarioService.iniciarSesion(datos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza el nombre y el correo de una cuenta")
    public UsuarioResponse actualizar(@PathVariable Long id, @RequestBody ActualizarUsuarioRequest datos) {
        return usuarioService.actualizar(id, datos);
    }

    @PutMapping("/{id}/contacto")
    @Operation(summary = "Guarda el correo secundario y el teléfono")
    public UsuarioResponse guardarContacto(@PathVariable Long id, @RequestBody ContactoRequest datos) {
        return usuarioService.guardarContacto(id, datos);
    }

    @PutMapping("/{id}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia la contraseña")
    public void cambiarContrasena(@PathVariable Long id, @RequestBody CambiarContrasenaRequest datos) {
        usuarioService.cambiarContrasena(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una cuenta de usuario final que no tenga cuotas por pagar")
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
    }
}
