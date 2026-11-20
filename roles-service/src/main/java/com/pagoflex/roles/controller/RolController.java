package com.pagoflex.roles.controller;

import com.pagoflex.roles.model.Rol;
import com.pagoflex.roles.service.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Roles de las cuentas de PagoFlex")
public class RolController {

    private final RolService rolService;

    @GetMapping
    @Operation(summary = "Lista todos los roles")
    public List<Rol> listar() {
        return rolService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un rol por su id")
    public Rol obtener(@PathVariable Long id) {
        return rolService.obtener(id);
    }
}
