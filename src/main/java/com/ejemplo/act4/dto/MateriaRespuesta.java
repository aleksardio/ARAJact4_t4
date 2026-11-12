package com.ejemplo.act4.dto;

import com.ejemplo.act4.entity.Materia;

public record MateriaRespuesta(Long id, String nombre, String docente, Integer semestre) {
	public static MateriaRespuesta de(Materia materia) {
		return new MateriaRespuesta(materia.getId(), materia.getNombre(), materia.getDocente(), materia.getSemestre());
	}
}
