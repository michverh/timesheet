package eu.openvalue.timesheet;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Connects tests to a throwaway PostgreSQL of the same version as used locally.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer("postgres:18");
  }
}
