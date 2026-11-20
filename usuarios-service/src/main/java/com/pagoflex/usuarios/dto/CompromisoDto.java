package com.pagoflex.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompromisoDto(String folio, String estado) {
}
