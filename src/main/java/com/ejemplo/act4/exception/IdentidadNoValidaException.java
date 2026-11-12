package com.ejemplo.act4.exception;

public class IdentidadNoValidaException extends RuntimeException {
	public IdentidadNoValidaException() {
		super("Se requiere una identidad de usuario válida");
	}
}
