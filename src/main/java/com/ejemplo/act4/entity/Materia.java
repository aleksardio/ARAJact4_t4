package com.ejemplo.act4.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "materias")
public class Materia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 255)
	@Column(nullable = false, length = 255)
	private String nombre;

	@NotBlank
	@Size(max = 255)
	@Column(nullable = false, length = 255)
	private String docente;

	@NotNull
	@Min(1)
	@Max(12)
	@Column(nullable = false)
	private Integer semestre;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	protected Materia() {
	}

	public Materia(String nombre, String docente, Integer semestre, Usuario usuario) {
		this.nombre = nombre;
		this.docente = docente;
		this.semestre = semestre;
		this.usuario = usuario;
	}

	public Long getId() { return id; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public String getDocente() { return docente; }
	public void setDocente(String docente) { this.docente = docente; }
	public Integer getSemestre() { return semestre; }
	public void setSemestre(Integer semestre) { this.semestre = semestre; }
	public Usuario getUsuario() { return usuario; }
	public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
