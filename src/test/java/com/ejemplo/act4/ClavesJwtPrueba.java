package com.ejemplo.act4;

import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Material efímero exclusivo de las pruebas, fuera del repositorio. */
abstract class ClavesJwtPrueba {
	private static final String RUTA = crearClave();
	private static String crearClave() {
		try {
			var generador = KeyPairGenerator.getInstance("RSA");
			generador.initialize(2048);
			var archivo = Files.createTempFile("act4-jwt-test-", ".pem");
			archivo.toFile().deleteOnExit();
			String clave = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(generador.generateKeyPair().getPrivate().getEncoded());
			Files.writeString(archivo, "-----BEGIN PRIVATE KEY-----\n" + clave + "\n-----END PRIVATE KEY-----\n");
			return archivo.toString();
		} catch (Exception ex) { throw new IllegalStateException(ex); }
	}
	@DynamicPropertySource
	static void propiedadesJwt(DynamicPropertyRegistry registro) {
		registro.add("app.jwt.private-key", () -> RUTA);
		registro.add("app.jwt.issuer", () -> "act4-test");
	}
}
