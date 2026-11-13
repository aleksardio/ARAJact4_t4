package com.ejemplo.act4.security;

import java.util.List;
import com.ejemplo.act4.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracionSeguridad {
	private record PrincipalJwt(Long usuarioId) implements IdentidadUsuario { }

	@Bean
	PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

	@Bean
	SecurityFilterChain seguridad(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
		AuthenticationEntryPoint sinAutenticacion = (request, response, ex) -> {
			response.setStatus(401);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"estado\":401,\"mensaje\":\"Se requiere autenticación válida\",\"campos\":{}}");
		};
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.requestCache(cache -> cache.disable())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.authorizeHttpRequests(reglas -> reglas
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers("/api/materias", "/api/materias/**").authenticated()
						.anyRequest().denyAll())
				.exceptionHandling(errores -> errores.authenticationEntryPoint(sinAutenticacion)
						.accessDeniedHandler((request, response, ex) -> {
							response.setStatus(403);
							response.setContentType("application/json");
							response.setCharacterEncoding("UTF-8");
							response.getWriter().write("{\"estado\":403,\"mensaje\":\"Acceso denegado\",\"campos\":{}}");
						}))
				.oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(sinAutenticacion)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
							long id = Long.parseLong(token.getSubject());
							if (!usuarios.existsById(id)) {
								throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
							}
							return UsernamePasswordAuthenticationToken.authenticated(new PrincipalJwt(id), null, List.of());
						})));
		return http.build();
	}
}
