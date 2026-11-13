package com.example.blooddonor.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base class for all integration tests executing against a real, isolated MySQL 8.0 instance
 * managed by Testcontainers.
 *
 * Spring Boot's {@link ServiceConnection} automatically configures datasource properties
 * (URL, username, password, driver) for the container.
 *
 * Flyway executes V1 and V2 migrations against this container, enforcing full schema validation
 * under {@code spring.jpa.hibernate.ddl-auto=validate}.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
public abstract class AbstractMySqlIT {

    @Container
    @ServiceConnection
    protected static final MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withDatabaseName("blood_donor_test")
            .withUsername("testuser")
            .withPassword("testpass");
}
