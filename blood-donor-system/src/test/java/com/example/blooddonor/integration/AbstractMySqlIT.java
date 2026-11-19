package com.example.blooddonor.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base class for all integration tests executing against a real, isolated MySQL 8.0 instance
 * managed by Testcontainers.
 *
 * Uses the Singleton Container pattern: the container is started once statically and kept
 * alive across all integration test classes, preventing intermediate container shutdown
 * between test classes.
 *
 * Spring Boot's {@link ServiceConnection} automatically configures datasource properties
 * (URL, username, password, driver) for the container.
 *
 * Flyway executes migrations against this container, enforcing full schema validation
 * under {@code spring.jpa.hibernate.ddl-auto=validate}.
 */
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.datasource.hikari.maximum-pool-size=50",
        "spring.datasource.hikari.minimum-idle=10",
        "spring.datasource.hikari.connection-timeout=30000"
})
public abstract class AbstractMySqlIT {

    @ServiceConnection
    protected static final MySQLContainer mysql;

    static {
        if (isDockerAvailable()) {
            mysql = new MySQLContainer("mysql:8.0")
                    .withDatabaseName("blood_donor_test")
                    .withUsername("testuser")
                    .withPassword("testpass");
            mysql.start();
        } else {
            mysql = null;
        }
    }

    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }
}
