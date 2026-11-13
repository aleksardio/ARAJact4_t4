package com.ejemplo.act4.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionJwtTests {
	@Test void rechazaClaveNoConfigurada() {
		var error = assertThrows(IllegalStateException.class, () -> new ConfiguracionJwt().clavesJwt(""));
		assertTrue(error.getMessage().contains("JWT_PRIVATE_KEY"));
	}
	@Test void rechazaClaveInexistente() {
		assertThrows(IllegalStateException.class, () -> new ConfiguracionJwt().clavesJwt("archivo-inexistente.pem"));
	}
}
