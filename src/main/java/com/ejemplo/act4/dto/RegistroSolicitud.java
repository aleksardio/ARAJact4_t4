package com.ejemplo.act4.dto;

import java.util.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroSolicitud(@NotBlank @Size(max = 255) String nombre,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8) String password) {
	public RegistroSolicitud {
		if (email != null) email = email.strip().toLowerCase(Locale.ROOT);
	}
	@Override public String toString() { return "RegistroSolicitud[datos protegidos]"; }
}
