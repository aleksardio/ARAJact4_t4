package com.ejemplo.act4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Act4ApplicationTests {

	@Test
	void applicationIsConfiguredAsSpringBootApplication() {
		assertTrue(Act4Application.class.isAnnotationPresent(SpringBootApplication.class));
	}

}
