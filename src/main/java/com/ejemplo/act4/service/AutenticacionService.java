package com.ejemplo.act4.service;

import java.nio.charset.StandardCharsets;
import com.ejemplo.act4.dto.LoginSolicitud;
import com.ejemplo.act4.dto.RegistroSolicitud;
import com.ejemplo.act4.dto.UsuarioRespuesta;
import com.ejemplo.act4.entity.Usuario;
import com.ejemplo.act4.exception.CredencialesInvalidasException;
import com.ejemplo.act4.exception.EmailDuplicadoException;
import com.ejemplo.act4.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {
	private final UsuarioRepository usuarios;
	private final PasswordEncoder encoder;
	private final String hashInexistente;

	public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder encoder) {
		this.usuarios = usuarios;
		this.encoder = encoder;
		this.hashInexistente = encoder.encode("credencial-no-utilizable");
	}

	public UsuarioRespuesta registrar(RegistroSolicitud solicitud) {
		if (solicitud.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException("Contraseña demasiado larga para BCrypt");
		}
		if (usuarios.existsByEmail(solicitud.email())) throw new EmailDuplicadoException();
		try {
			Usuario usuario = new Usuario(solicitud.nombre(), solicitud.email(), encoder.encode(solicitud.password()));
			return UsuarioRespuesta.de(usuarios.saveAndFlush(usuario));
		} catch (DataIntegrityViolationException ex) {
			// La transacción del repositorio ya terminó; comprobar sin ocultar otros fallos de integridad.
			if (usuarios.existsByEmail(solicitud.email())) throw new EmailDuplicadoException();
			throw ex;
		}
	}

	public UsuarioRespuesta verificarCredenciales(LoginSolicitud solicitud) {
		Usuario usuario = usuarios.findByEmail(solicitud.email()).orElse(null);
		String hash = usuario == null ? hashInexistente : usuario.getPasswordHash();
		boolean coincide = solicitud.password() != null
				&& solicitud.password().getBytes(StandardCharsets.UTF_8).length <= 72
				&& encoder.matches(solicitud.password(), hash);
		if (usuario == null || !coincide) throw new CredencialesInvalidasException();
		return UsuarioRespuesta.de(usuario);
	}
}
