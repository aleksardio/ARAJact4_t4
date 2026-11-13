package com.ejemplo.act4;

import com.ejemplo.act4.entity.Materia;
import com.ejemplo.act4.entity.Usuario;
import com.ejemplo.act4.repository.MateriaRepository;
import com.ejemplo.act4.repository.UsuarioRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:act4test;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=file:database/schema.sql",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class PersistenciaIntegrationTests extends ClavesJwtPrueba {

	@Autowired private UsuarioRepository usuarios;
	@Autowired private MateriaRepository materias;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private Validator validator;

	@BeforeEach
	void limpiarBaseEnMemoria() {
		jdbc.update("DELETE FROM materias");
		jdbc.update("DELETE FROM usuarios");
	}

	@Test
	void guardaYConsultaPorPropietarioConPaginacion() {
		Usuario ana = usuarios.saveAndFlush(new Usuario("Ana", "ana@example.test", "hash-de-prueba"));
		Usuario luis = usuarios.saveAndFlush(new Usuario("Luis", "luis@example.test", "hash-de-prueba"));
		Materia algebra = materias.saveAndFlush(new Materia("Álgebra", "Docente A", 1, ana));
		materias.saveAndFlush(new Materia("Historia", "Docente B", 12, ana));
		materias.saveAndFlush(new Materia("Física", "Docente C", 2, luis));

		assertEquals(ana.getId(), usuarios.findByEmail("ana@example.test").orElseThrow().getId());
		assertTrue(usuarios.existsByEmail("ana@example.test"));
		assertFalse(usuarios.existsByEmail("ausente@example.test"));
		var pagina = materias.findByUsuarioId(ana.getId(), PageRequest.of(0, 1));
		assertEquals(2, pagina.getTotalElements());
		assertEquals(1, pagina.getNumberOfElements());
		assertTrue(materias.findByIdAndUsuarioId(algebra.getId(), ana.getId()).isPresent());
		assertTrue(materias.findByIdAndUsuarioId(algebra.getId(), luis.getId()).isEmpty());
	}

	@Test
	void validaCamposYLimitesEnLasEntidades() {
		Usuario usuario = new Usuario(" ", "no-es-email", " ");
		assertEquals(3, validator.validate(usuario).size());
		Materia materia = new Materia("Materia", "Docente", 0, null);
		assertEquals(2, validator.validate(materia).size());
		materia.setSemestre(13);
		assertEquals(2, validator.validate(materia).size());
		materia.setUsuario(new Usuario("Ana", "ana@example.test", "hash-de-prueba"));
		materia.setSemestre(1);
		assertTrue(validator.validate(materia).isEmpty());
		materia.setSemestre(12);
		assertTrue(validator.validate(materia).isEmpty());
	}

	@Test
	void baseExigeUnicidadPropietarioYSemestreValido() {
		Usuario usuario = usuarios.saveAndFlush(new Usuario("Ana", "ana@example.test", "hash-de-prueba"));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO usuarios (nombre, email, password_hash) VALUES (?, ?, ?)",
				"Otra", "ana@example.test", "hash-de-prueba"));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO materias (nombre, docente, semestre, usuario_id) VALUES (?, ?, ?, ?)",
				"Materia", "Docente", 1, null));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO materias (nombre, docente, semestre, usuario_id) VALUES (?, ?, ?, ?)",
				"Materia", "Docente", 1, -1));
		for (int semestre : new int[] {0, 13}) {
			assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
					"INSERT INTO materias (nombre, docente, semestre, usuario_id) VALUES (?, ?, ?, ?)",
					"Materia", "Docente", semestre, usuario.getId()));
		}
	}
}
