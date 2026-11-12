package com.ejemplo.act4;

import java.util.List;

import com.ejemplo.act4.entity.Materia;
import com.ejemplo.act4.entity.Usuario;
import com.ejemplo.act4.repository.MateriaRepository;
import com.ejemplo.act4.repository.UsuarioRepository;
import com.ejemplo.act4.security.IdentidadUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:act4apitest;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa", "spring.datasource.password=",
		"spring.sql.init.mode=always", "spring.sql.init.schema-locations=file:database/schema.sql",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class MateriaApiIntegrationTests {
	@Autowired private MockMvc mvc;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private MateriaRepository materias;
	@Autowired private JdbcTemplate jdbc;
	private Usuario ana;
	private Usuario luis;
	private Materia propia;
	private Materia ajena;
	private static final String JSON = "{\"nombre\":\"Química\",\"docente\":\"Docente\",\"semestre\":3}";

	private record PrincipalPrueba(Long usuarioId) implements IdentidadUsuario { }

	@BeforeEach
	void preparar() {
		jdbc.update("DELETE FROM materias");
		jdbc.update("DELETE FROM usuarios");
		ana = usuarios.saveAndFlush(new Usuario("Ana", "ana@example.test", "hash-de-prueba"));
		luis = usuarios.saveAndFlush(new Usuario("Luis", "luis@example.test", "hash-de-prueba"));
		propia = materias.saveAndFlush(new Materia("Álgebra", "Docente A", 1, ana));
		materias.saveAndFlush(new Materia("Historia", "Docente B", 2, ana));
		ajena = materias.saveAndFlush(new Materia("Física", "Docente C", 3, luis));
	}

	private RequestPostProcessor identidad(Long id) {
		var auth = new UsernamePasswordAuthenticationToken(new PrincipalPrueba(id), "no-usada",
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		return authentication(auth);
	}

	@Test
	void listarPaginadoYConsultar() throws Exception {
		mvc.perform(get("/api/materias?page=0&size=1").with(identidad(ana.getId())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.contenido.length()").value(1))
				.andExpect(jsonPath("$.contenido[0].id").value(propia.getId()))
				.andExpect(jsonPath("$.contenido[0].usuario").doesNotExist())
				.andExpect(jsonPath("$.contenido[0].passwordHash").doesNotExist());
		mvc.perform(get("/api/materias/{id}", propia.getId()).with(identidad(ana.getId())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Álgebra"));
		mvc.perform(get("/api/materias/{id}", Long.MAX_VALUE).with(identidad(ana.getId())))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.estado").value(404));
	}

	@Test
	void crearActualizarYEliminar() throws Exception {
		mvc.perform(post("/api/materias").with(identidad(ana.getId())).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(JSON))
				.andExpect(status().isCreated()).andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.nombre").value("Química"))
				.andExpect(jsonPath("$.usuario").doesNotExist());
		assertEquals(3, materias.findByUsuarioId(ana.getId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
		mvc.perform(put("/api/materias/{id}", propia.getId()).with(identidad(ana.getId())).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Química"));
		assertEquals("Química", materias.findById(propia.getId()).orElseThrow().getNombre());
		mvc.perform(delete("/api/materias/{id}", propia.getId()).with(identidad(ana.getId())).with(csrf()))
				.andExpect(status().isNoContent()).andExpect(content().string(""));
		assertFalse(materias.existsById(propia.getId()));
	}

	@Test
	void noPermiteAccederNiModificarMateriasAjenas() throws Exception {
		mvc.perform(get("/api/materias/{id}", ajena.getId()).with(identidad(ana.getId())))
				.andExpect(status().isNotFound());
		mvc.perform(put("/api/materias/{id}", ajena.getId()).with(identidad(ana.getId())).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(JSON)).andExpect(status().isNotFound());
		mvc.perform(delete("/api/materias/{id}", ajena.getId()).with(identidad(ana.getId())).with(csrf()))
				.andExpect(status().isNotFound());
		assertEquals("Física", materias.findById(ajena.getId()).orElseThrow().getNombre());
		mvc.perform(post("/api/materias").with(identidad(ana.getId())).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Nueva\",\"docente\":\"D\",\"semestre\":1,\"usuarioId\":" + luis.getId() + "}"))
				.andExpect(status().isCreated());
		assertEquals(1, materias.findByUsuarioId(luis.getId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
		assertEquals(3, materias.findByUsuarioId(ana.getId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
	}

	@Test
	void rechazaDatosInvalidosYPaginacionIncorrecta() throws Exception {
		for (String cuerpo : List.of("{}", "{\"nombre\":\" \",\"docente\":\" \",\"semestre\":1}",
				"{\"nombre\":\"N\",\"docente\":\"D\",\"semestre\":0}",
				"{\"nombre\":\"N\",\"docente\":\"D\",\"semestre\":13}",
				"{\"nombre\":\"N\",\"docente\":\"D\",\"semestre\":1.5}")) {
			mvc.perform(post("/api/materias").with(identidad(ana.getId())).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.estado").value(400));
		}
		for (String query : List.of("page=-1", "size=0", "size=101", "page=xyz", "page=2147483647&size=100")) {
			mvc.perform(get("/api/materias?" + query).with(identidad(ana.getId())))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.estado").value(400));
		}
	}

	@Test
	void rechazaAnonimosYPrincipalesNoConfiables() throws Exception {
		mvc.perform(get("/api/materias").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/materias").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/materias").with(user("ana@example.test")))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/materias").with(identidad(Long.MAX_VALUE)))
				.andExpect(status().isUnauthorized());
	}
}
