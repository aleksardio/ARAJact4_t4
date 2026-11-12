package com.ejemplo.act4.controller;

import java.net.URI;

import com.ejemplo.act4.dto.MateriaRespuesta;
import com.ejemplo.act4.dto.MateriaSolicitud;
import com.ejemplo.act4.dto.PaginaMaterias;
import com.ejemplo.act4.exception.IdentidadNoValidaException;
import com.ejemplo.act4.security.IdentidadUsuario;
import com.ejemplo.act4.service.MateriaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materias")
public class MateriaController {
	private final MateriaService service;

	public MateriaController(MateriaService service) { this.service = service; }

	@GetMapping
	public PaginaMaterias listar(Authentication autenticacion,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Paginación inválida: page debe ser >= 0 y size estar entre 1 y 100");
		}
		return PaginaMaterias.de(service.listar(usuarioId(autenticacion), PageRequest.of(page, size, Sort.by("id"))));
	}

	@GetMapping("/{id}")
	public MateriaRespuesta consultar(Authentication autenticacion, @PathVariable Long id) {
		return service.consultar(usuarioId(autenticacion), id);
	}

	@PostMapping
	public ResponseEntity<MateriaRespuesta> crear(Authentication autenticacion, @Valid @RequestBody MateriaSolicitud solicitud) {
		MateriaRespuesta respuesta = service.crear(usuarioId(autenticacion), solicitud);
		return ResponseEntity.created(URI.create("/api/materias/" + respuesta.id())).body(respuesta);
	}

	@PutMapping("/{id}")
	public MateriaRespuesta actualizar(Authentication autenticacion, @PathVariable Long id,
			@Valid @RequestBody MateriaSolicitud solicitud) {
		return service.actualizar(usuarioId(autenticacion), id, solicitud);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(Authentication autenticacion, @PathVariable Long id) {
		service.eliminar(usuarioId(autenticacion), id);
		return ResponseEntity.noContent().build();
	}

	private Long usuarioId(Authentication autenticacion) {
		if (autenticacion == null || !autenticacion.isAuthenticated()
				|| autenticacion instanceof AnonymousAuthenticationToken
				|| !(autenticacion.getPrincipal() instanceof IdentidadUsuario identidad)
				|| identidad.usuarioId() == null || identidad.usuarioId() <= 0) {
			throw new IdentidadNoValidaException();
		}
		return identidad.usuarioId();
	}
}
