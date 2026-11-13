package com.ejemplo.act4.exception;

public class EmailDuplicadoException extends RuntimeException {
	public EmailDuplicadoException() { super("El correo ya está registrado"); }
}
