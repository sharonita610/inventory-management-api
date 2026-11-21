package com.inventory.support;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

@SpringBootTest
public abstract class IntegrationTest {

	private static final EmbeddedPostgres POSTGRES = startPostgres();

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void datasourceProperties(final DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "");
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> 20);
	}

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("TRUNCATE TABLE stock_history, product RESTART IDENTITY CASCADE");
	}

	private static EmbeddedPostgres startPostgres() {
		try {
			return EmbeddedPostgres.builder().start();
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
