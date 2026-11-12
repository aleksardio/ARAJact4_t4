package com.ejemplo.act4.repository;

import java.util.Optional;

import com.ejemplo.act4.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByEmail(String email);
	boolean existsByEmail(String email);
}
