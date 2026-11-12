package com.ejemplo.act4.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MateriaSolicitud(
		@NotBlank @Size(max = 255) String nombre,
		@NotBlank @Size(max = 255) String docente,
		@NotNull @Min(1) @Max(12) @Digits(integer = 2, fraction = 0) BigDecimal semestre) {
}
