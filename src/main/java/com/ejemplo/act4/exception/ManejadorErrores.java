package com.ejemplo.act4.exception;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorErrores {
	public record ErrorRespuesta(int estado, String mensaje, Map<String, String> campos) { }

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
		Map<String, String> campos = new TreeMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos", campos);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ErrorRespuesta> formato() {
		return respuesta(HttpStatus.BAD_REQUEST, "Formato de solicitud inválido", Map.of());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorRespuesta> argumento() {
		return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", Map.of());
	}

	@ExceptionHandler(MateriaNoEncontradaException.class)
	public ResponseEntity<ErrorRespuesta> noEncontrada() {
		return respuesta(HttpStatus.NOT_FOUND, "Materia no encontrada", Map.of());
	}

	@ExceptionHandler(IdentidadNoValidaException.class)
	public ResponseEntity<ErrorRespuesta> identidad() {
		return respuesta(HttpStatus.UNAUTHORIZED, "Se requiere una identidad de usuario válida", Map.of());
	}

	private ResponseEntity<ErrorRespuesta> respuesta(HttpStatus estado, String mensaje, Map<String, String> campos) {
		return ResponseEntity.status(estado).body(new ErrorRespuesta(estado.value(), mensaje, campos));
	}
}
