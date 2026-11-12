package com.ejemplo.act4.repository;

import java.util.Optional;

import com.ejemplo.act4.entity.Materia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
	Page<Materia> findByUsuarioId(Long usuarioId, Pageable pageable);
	Optional<Materia> findByIdAndUsuarioId(Long id, Long usuarioId);
}
