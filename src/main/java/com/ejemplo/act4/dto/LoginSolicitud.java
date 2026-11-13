package com.ejemplo.act4.dto;

import java.util.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginSolicitud(@NotBlank @Email @Size(max = 255) String email,
		@NotBlank String password) {
	public LoginSolicitud {
		if (email != null) email = email.strip().toLowerCase(Locale.ROOT);
	}
	@Override public String toString() { return "LoginSolicitud[datos protegidos]"; }
}
