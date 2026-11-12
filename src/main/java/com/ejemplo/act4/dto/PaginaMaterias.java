package com.ejemplo.act4.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record PaginaMaterias(List<MateriaRespuesta> contenido, int pagina, int tamano,
		long totalElementos, int totalPaginas) {
	public static PaginaMaterias de(Page<MateriaRespuesta> pagina) {
		return new PaginaMaterias(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
				pagina.getTotalElements(), pagina.getTotalPages());
	}
}
