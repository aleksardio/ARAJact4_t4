package com.ejemplo.act4.controller;

import com.ejemplo.act4.dto.RegistroSolicitud;
import com.ejemplo.act4.dto.UsuarioRespuesta;
import com.ejemplo.act4.service.AutenticacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {
	private final AutenticacionService servicio;
	public AutenticacionController(AutenticacionService servicio) { this.servicio = servicio; }

	@PostMapping("/register")
	public ResponseEntity<UsuarioRespuesta> registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
	}
}
