package com.ejemplo.act4;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import com.ejemplo.act4.dto.LoginSolicitud;
import com.ejemplo.act4.exception.CredencialesInvalidasException;
import com.ejemplo.act4.repository.UsuarioRepository;
import com.ejemplo.act4.service.AutenticacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:act4authtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.sql.init.mode=always", "spring.sql.init.schema-locations=file:database/schema.sql",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class AutenticacionIntegrationTests extends ClavesJwtPrueba {
	@Autowired private MockMvc mvc;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private PasswordEncoder encoder;
	@Autowired private AutenticacionService servicio;
	@Autowired private JdbcTemplate jdbc;
	private static final String VALIDO = "{\"nombre\":\"Ana\",\"email\":\"  ANA@example.test  \",\"password\":\"clave-segura\"}";

	@BeforeEach void limpiar() {
		jdbc.update("DELETE FROM materias");
		jdbc.update("DELETE FROM usuarios");
	}

	@Test void registraConHashYAutentica() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Ana"))
				.andExpect(jsonPath("$.email").value("ana@example.test"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		var usuario = usuarios.findByEmail("ana@example.test").orElseThrow();
		assertTrue(usuario.getPasswordHash().startsWith("$2a$"));
		assertNotEquals("clave-segura", usuario.getPasswordHash());
		assertTrue(encoder.matches("clave-segura", usuario.getPasswordHash()));
		assertFalse(encoder.matches("incorrecta", usuario.getPasswordHash()));
		assertEquals(usuario.getId(), servicio.verificarCredenciales(new LoginSolicitud(" ANA@example.test ", "clave-segura")).id());
		var error1 = assertThrows(CredencialesInvalidasException.class,
				() -> servicio.verificarCredenciales(new LoginSolicitud("ana@example.test", "incorrecta")));
		var error2 = assertThrows(CredencialesInvalidasException.class,
				() -> servicio.verificarCredenciales(new LoginSolicitud("nadie@example.test", "incorrecta")));
		assertEquals(error1.getMessage(), error2.getMessage());
		assertThrows(CredencialesInvalidasException.class,
				() -> servicio.verificarCredenciales(new LoginSolicitud("ana@example.test", "clave-segura" + "x".repeat(80))));
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.estado").value(409));
	}

	@Test void rechazaEntradasInvalidas() throws Exception {
		for (String cuerpo : List.of("{}",
				"{\"nombre\":\"Ana\",\"email\":\"no-es-email\",\"password\":\"12345678\"}",
				"{\"nombre\":\"  \",\"email\":\"a@example.test\",\"password\":\"12345678\"}",
				"{\"nombre\":\"Ana\",\"email\":\"a@example.test\",\"password\":\"1234567\"}",
				"{\"nombre\":\"Ana\",\"email\":\"a@example.test\",\"password\":\"" + "é".repeat(37) + "\"}")) {
			mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.estado").value(400));
		}
		assertEquals(0, usuarios.count());
	}

	@Test void duplicadosConcurrentesProducenUnSoloRegistro() throws Exception {
		var inicio = new CountDownLatch(1);
		var pool = Executors.newFixedThreadPool(2);
		try {
			var tarea = (java.util.concurrent.Callable<Integer>) () -> {
				inicio.await();
				return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
						.andReturn().getResponse().getStatus();
			};
			var a = pool.submit(tarea);
			var b = pool.submit(tarea);
			inicio.countDown();
			assertEquals(List.of(201, 409), java.util.stream.Stream.of(a.get(), b.get()).sorted().toList());
			assertEquals(1, usuarios.count());
		} finally { pool.shutdownNow(); }
	}

	@Test void rutasProtegidasYLoginPublico() throws Exception {
		mvc.perform(get("/api/materias")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.estado").value(401));
		mvc.perform(post("/api/materias").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}
}
