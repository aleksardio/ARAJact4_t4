package com.ejemplo.act4.service;

import com.ejemplo.act4.dto.MateriaRespuesta;
import com.ejemplo.act4.dto.MateriaSolicitud;
import com.ejemplo.act4.entity.Materia;
import com.ejemplo.act4.entity.Usuario;
import com.ejemplo.act4.exception.IdentidadNoValidaException;
import com.ejemplo.act4.exception.MateriaNoEncontradaException;
import com.ejemplo.act4.repository.MateriaRepository;
import com.ejemplo.act4.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateriaService {
	private final MateriaRepository materias;
	private final UsuarioRepository usuarios;

	public MateriaService(MateriaRepository materias, UsuarioRepository usuarios) {
		this.materias = materias;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public Page<MateriaRespuesta> listar(Long usuarioId, Pageable pageable) {
		validarUsuario(usuarioId);
		return materias.findByUsuarioId(usuarioId, pageable).map(MateriaRespuesta::de);
	}

	@Transactional(readOnly = true)
	public MateriaRespuesta consultar(Long usuarioId, Long id) {
		validarUsuario(usuarioId);
		return MateriaRespuesta.de(propia(usuarioId, id));
	}

	@Transactional
	public MateriaRespuesta crear(Long usuarioId, MateriaSolicitud solicitud) {
		Usuario usuario = validarUsuario(usuarioId);
		Materia materia = new Materia(solicitud.nombre(), solicitud.docente(), solicitud.semestre().intValueExact(), usuario);
		return MateriaRespuesta.de(materias.save(materia));
	}

	@Transactional
	public MateriaRespuesta actualizar(Long usuarioId, Long id, MateriaSolicitud solicitud) {
		validarUsuario(usuarioId);
		Materia materia = propia(usuarioId, id);
		materia.setNombre(solicitud.nombre());
		materia.setDocente(solicitud.docente());
		materia.setSemestre(solicitud.semestre().intValueExact());
		return MateriaRespuesta.de(materia);
	}

	@Transactional
	public void eliminar(Long usuarioId, Long id) {
		validarUsuario(usuarioId);
		materias.delete(propia(usuarioId, id));
	}

	private Usuario validarUsuario(Long id) {
		if (id == null || id <= 0) throw new IdentidadNoValidaException();
		return usuarios.findById(id).orElseThrow(IdentidadNoValidaException::new);
	}

	private Materia propia(Long usuarioId, Long id) {
		return materias.findByIdAndUsuarioId(id, usuarioId).orElseThrow(MateriaNoEncontradaException::new);
	}
}
