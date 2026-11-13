package com.ejemplo.act4.dto;

import com.ejemplo.act4.entity.Usuario;

public record UsuarioRespuesta(Long id, String nombre, String email) {
	public static UsuarioRespuesta de(Usuario usuario) {
		return new UsuarioRespuesta(usuario.getId(), usuario.getNombre(), usuario.getEmail());
	}
}
