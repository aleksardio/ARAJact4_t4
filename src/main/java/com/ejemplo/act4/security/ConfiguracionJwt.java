package com.ejemplo.act4.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class ConfiguracionJwt {
	@Bean
	KeyPair clavesJwt(@Value("${app.jwt.private-key:}") String archivo) {
		if (archivo.isBlank()) throw new IllegalStateException("Configure JWT_PRIVATE_KEY con la ruta de una clave RSA PKCS#8 PEM");
		try {
			String pem = Files.readString(Path.of(archivo));
			String contenido = pem.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
			var privada = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(
					new PKCS8EncodedKeySpec(Base64.getDecoder().decode(contenido)));
			if (privada.getModulus().bitLength() < 2048) throw new IllegalArgumentException("RSA requiere al menos 2048 bits");
			var publica = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
					new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
			return new KeyPair(publica, privada);
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo cargar una clave RSA PKCS#8 PEM válida de al menos 2048 bits", ex);
		}
	}

	@Bean
	JwtEncoder jwtEncoder(KeyPair claves) {
		RSAKey jwk = new RSAKey.Builder((RSAPublicKey) claves.getPublic())
				.privateKey(claves.getPrivate()).build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
	}

	@Bean
	JwtDecoder jwtDecoder(KeyPair claves, @Value("${app.jwt.issuer}") String emisor) {
		if (emisor.isBlank()) throw new IllegalStateException("El emisor JWT no puede estar vacío");
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) claves.getPublic())
				.signatureAlgorithm(SignatureAlgorithm.RS256).build();
		OAuth2TokenValidator<Jwt> claims = jwt -> {
			Instant emitido = jwt.getIssuedAt();
			Instant expiracion = jwt.getExpiresAt();
			String sub = jwt.getSubject();
			boolean valido = emitido != null && expiracion != null && expiracion.isAfter(emitido)
					&& !emitido.isAfter(Instant.now()) && sub != null && sub.matches("[1-9][0-9]*");
			if (valido) {
				try { Long.parseLong(sub); } catch (NumberFormatException ex) { valido = false; }
			}
			return valido ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
					new OAuth2Error("invalid_token", "Claims inválidos", null));
		};
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(emisor), claims));
		return decoder;
	}
}
