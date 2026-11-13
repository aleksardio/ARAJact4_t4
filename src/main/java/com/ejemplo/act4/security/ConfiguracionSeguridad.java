package com.ejemplo.act4.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracionSeguridad {
	@Bean
	PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

	@Bean
	SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.requestCache(cache -> cache.disable())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.authorizeHttpRequests(reglas -> reglas
						.requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(errores -> errores.authenticationEntryPoint((request, response, ex) -> {
					response.setStatus(401);
					response.setContentType("application/json");
					response.getWriter().write("{\"estado\":401,\"mensaje\":\"Se requiere autenticación\",\"campos\":{}}");
				}));
		return http.build();
	}
}
