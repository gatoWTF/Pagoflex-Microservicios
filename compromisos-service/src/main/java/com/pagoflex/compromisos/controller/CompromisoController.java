package com.pagoflex.compromisos.controller;

import com.pagoflex.compromisos.dto.AnularRequest;
import com.pagoflex.compromisos.dto.CambiarEstadoRequest;
import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.dto.NuevoCompromisoRequest;
import com.pagoflex.compromisos.dto.NombreTitularRequest;
import com.pagoflex.compromisos.dto.PagoRequest;
import com.pagoflex.compromisos.dto.PagoResponse;
import com.pagoflex.compromisos.service.CompromisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/compromisos")
@RequiredArgsConstructor
@Tag(name = "Compromisos", description = "Cuotas por pagar, pagos y anulaciones")
public class CompromisoController {

    private final CompromisoService compromisoService;

    @GetMapping
    @Operation(summary = "Lista los compromisos de un usuario (rut) o de una empresa (empresa)")
    public List<CompromisoResponse> listar(@RequestParam(required = false) String rut,
                                           @RequestParam(required = false) String empresa) {
        if (rut != null && !rut.isBlank()) {
            return compromisoService.listarPorUsuario(rut);
        }
        if (empresa != null && !empresa.isBlank()) {
            return compromisoService.listarPorEmpresa(empresa);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el rut del usuario o el codigo de la empresa");
    }

    @GetMapping("/{folio}")
    @Operation(summary = "Obtiene un compromiso por su folio")
    public CompromisoResponse obtener(@PathVariable String folio) {
        return compromisoService.obtener(folio);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra la primera cuota de un crédito (ejecutivo)")
    public CompromisoResponse registrar(@RequestBody NuevoCompromisoRequest datos) {
        return compromisoService.registrar(datos);
    }

    @PutMapping("/{folio}/estado")
    @Operation(summary = "Cambia el estado: en revisión, pendiente o vencido")
    public CompromisoResponse cambiarEstado(@PathVariable String folio, @RequestBody CambiarEstadoRequest datos) {
        return compromisoService.cambiarEstado(folio, datos);
    }

    @PutMapping("/{folio}/anular")
    @Operation(summary = "Anula un compromiso indicando el motivo (ejecutivo)")
    public CompromisoResponse anular(@PathVariable String folio, @RequestBody AnularRequest datos) {
        return compromisoService.anular(folio, datos);
    }

    @PutMapping("/titular/{rut}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Actualiza el nombre del titular en todos sus compromisos")
    public void actualizarNombreTitular(@PathVariable String rut, @RequestBody NombreTitularRequest datos) {
        compromisoService.actualizarNombreTitular(rut, datos);
    }

    @PostMapping("/{folio}/pagar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Paga una cuota, entrega el comprobante y genera la siguiente cuota")
    public PagoResponse pagar(@PathVariable String folio, @RequestBody(required = false) PagoRequest datos) {
        return compromisoService.pagar(folio, datos);
    }
}
