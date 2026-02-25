package com.rajathgoku.agentic.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

import com.rajathgoku.agentic.backend.llm.LlmClient;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
		// Test that the Spring application context loads successfully
		// Uses actual PostgreSQL database from Docker containers
		// LLM client is mocked to avoid AWS API calls during tests
	}

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public LlmClient llmClient() {
			return Mockito.mock(LlmClient.class);
		}
	}
}
