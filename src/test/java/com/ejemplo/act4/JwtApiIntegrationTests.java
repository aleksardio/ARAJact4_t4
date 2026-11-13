package com.ejemplo.act4;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.ejemplo.act4.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:act4jwttest;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.sql.init.mode=always", "spring.sql.init.schema-locations=file:database/schema.sql",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class JwtApiIntegrationTests extends ClavesJwtPrueba {
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;
	@Autowired UsuarioRepository usuarios;
	@Autowired JwtDecoder decoder;
	@Autowired JwtEncoder encoder;
	private String ana;
	private String luis;
	private Long anaId;
	private Long luisId;

	@BeforeEach void preparar() throws Exception {
		jdbc.update("DELETE FROM materias");
		jdbc.update("DELETE FROM usuarios");
		registrar("ana@example.test");
		registrar("luis@example.test");
		anaId = usuarios.findByEmail("ana@example.test").orElseThrow().getId();
		luisId = usuarios.findByEmail("luis@example.test").orElseThrow().getId();
		ana = login(" ANA@example.test ");
		luis = login("luis@example.test");
	}

	private void registrar(String email) throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Prueba\",\"email\":\"" + email + "\",\"password\":\"clave-segura\"}"))
				.andExpect(status().isCreated());
	}
	private String login(String email) throws Exception {
		String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"clave-segura\"}"))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}
	private String firmado(JwtEncoder firmante, String sub, String iss, Instant iat, Instant exp) {
		var claims = JwtClaimsSet.builder().issuer(iss).subject(sub).issuedAt(iat).expiresAt(exp).build();
		return firmante.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
	}
	private void rechazado(String token) throws Exception {
		mvc.perform(get("/api/materias").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.estado").value(401)).andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test void loginEmiteJwtRs256VerificableConClaims() {
		Jwt token = decoder.decode(ana);
		assertEquals("RS256", token.getHeaders().get("alg"));
		assertEquals(anaId.toString(), token.getSubject());
		assertEquals("act4-test", token.getClaimAsString("iss"));
		assertEquals(3600, token.getExpiresAt().getEpochSecond() - token.getIssuedAt().getEpochSecond());
	}

	@Test void credencialesInvalidasYValidacion() throws Exception {
		String a = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ana@example.test\",\"password\":\"incorrecta\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.estado").value(401))
				.andReturn().getResponse().getContentAsString();
		String b = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ausente@example.test\",\"password\":\"incorrecta\"}"))
				.andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
		assertEquals(a, b);
		assertFalse(a.contains("incorrecta"));
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test void tokensInvalidosSonRechazados() throws Exception {
		Instant ahora = Instant.now();
		rechazado(firmado(encoder, anaId.toString(), "act4-test", ahora.minusSeconds(120), ahora.minusSeconds(60)));
		rechazado(firmado(encoder, anaId.toString(), "otro-emisor", ahora.minusSeconds(5), ahora.plusSeconds(60)));
		rechazado(firmado(encoder, "0", "act4-test", ahora.minusSeconds(5), ahora.plusSeconds(60)));
		rechazado(firmado(encoder, anaId.toString(), "act4-test", ahora.plusSeconds(120), ahora.plusSeconds(180)));
		var sinExp = JwtClaimsSet.builder().issuer("act4-test").subject(anaId.toString()).issuedAt(ahora).build();
		rechazado(encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), sinExp)).getTokenValue());
		rechazado("eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0.");
		rechazado(firmado(encoder, Long.toString(Long.MAX_VALUE), "act4-test", ahora.minusSeconds(5), ahora.plusSeconds(60)));
		var generador = KeyPairGenerator.getInstance("RSA");
		generador.initialize(2048);
		var par = generador.generateKeyPair();
		var jwk = new RSAKey.Builder((RSAPublicKey) par.getPublic()).privateKey(par.getPrivate()).build();
		rechazado(firmado(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk))), anaId.toString(),
				"act4-test", ahora.minusSeconds(5), ahora.plusSeconds(60)));
		rechazado("no.es.jwt");
		mvc.perform(get("/api/materias")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.estado").value(401));
		mvc.perform(get("/ruta-no-habilitada").header("Authorization", "Bearer " + ana))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.estado").value(403));
	}

	@Test void crudYPropiedadConBearerReal() throws Exception {
		String json = "{\"nombre\":\"Materia\",\"docente\":\"D\",\"semestre\":2,\"usuarioId\":" + luisId + "}";
		String respuesta = mvc.perform(post("/api/materias").header("Authorization", "Bearer " + ana)
				.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		int id = JsonPath.read(respuesta, "$.id");
		assertEquals(anaId.longValue(), jdbc.queryForObject("SELECT usuario_id FROM materias WHERE id = ?", Long.class, id));
		mvc.perform(get("/api/materias").header("Authorization", "Bearer " + ana))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
		mvc.perform(get("/api/materias/" + id).header("Authorization", "Bearer " + luis)).andExpect(status().isNotFound());
		mvc.perform(put("/api/materias/" + id).header("Authorization", "Bearer " + luis)
				.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isNotFound());
		mvc.perform(delete("/api/materias/" + id).header("Authorization", "Bearer " + luis)).andExpect(status().isNotFound());
		mvc.perform(put("/api/materias/" + id).header("Authorization", "Bearer " + ana)
				.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
		mvc.perform(delete("/api/materias/" + id).header("Authorization", "Bearer " + ana)).andExpect(status().isNoContent());
	}
}
