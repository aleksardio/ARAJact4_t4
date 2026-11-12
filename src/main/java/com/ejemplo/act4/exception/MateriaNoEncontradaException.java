package com.ejemplo.act4.exception;

public class MateriaNoEncontradaException extends RuntimeException {
	public MateriaNoEncontradaException() {
		super("Materia no encontrada");
	}
}
