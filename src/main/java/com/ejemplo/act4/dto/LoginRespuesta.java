package com.ejemplo.act4.dto;

public record LoginRespuesta(String accessToken, String tokenType, long expiresIn) {
	@Override public String toString() { return "LoginRespuesta[token protegido]"; }
}
