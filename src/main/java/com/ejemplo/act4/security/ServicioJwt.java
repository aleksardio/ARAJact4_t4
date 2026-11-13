package com.ejemplo.act4.security;

import java.time.Instant;
import com.ejemplo.act4.dto.LoginRespuesta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class ServicioJwt {
	private static final long DURACION_SEGUNDOS = 3600;
	private final JwtEncoder encoder;
	private final String emisor;
	public ServicioJwt(JwtEncoder encoder, @Value("${app.jwt.issuer}") String emisor) {
		this.encoder = encoder;
		this.emisor = emisor;
	}
	public LoginRespuesta emitir(Long usuarioId) {
		Instant ahora = Instant.now();
		var claims = JwtClaimsSet.builder().issuer(emisor).subject(usuarioId.toString())
				.issuedAt(ahora).expiresAt(ahora.plusSeconds(DURACION_SEGUNDOS)).build();
		String token = encoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
		return new LoginRespuesta(token, "Bearer", DURACION_SEGUNDOS);
	}
}
