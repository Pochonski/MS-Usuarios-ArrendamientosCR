package com.arrendamientos.usuarios.testsupport;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Levanta un contenedor de SQL Server 2022 para los tests de integración.
 * Configura el DataSource dinámicamente vía DynamicPropertyRegistry.
 *
 * Sólo se inicia si Docker está disponible; en caso contrario, los tests
 * marcados con @EnabledIfDockerAvailable son saltados (skip).
 */
public class MsSqlTestContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/mssql/server:2022-latest");

    public static final MSSQLServerContainer<?> CONTAINER;

    static {
        if (isDockerAvailable()) {
            CONTAINER = new MSSQLServerContainer<>(IMAGE)
                    .acceptLicense()
                    .withPassword("YourStrong!Passw0rd")
                    .withDatabaseName("usuarios_db")
                    .withReuse(true);
            CONTAINER.start();
            Runtime.getRuntime().addShutdownHook(new Thread(CONTAINER::stop));
        } else {
            CONTAINER = null;
        }
    }

    private static boolean isDockerAvailable() {
        try {
            String skip = System.getenv("SKIP_DOCKER_TESTS");
            if ("true".equalsIgnoreCase(skip)) {
                return false;
            }
            String dockerHost = System.getenv("DOCKER_HOST");
            if (dockerHost != null && !dockerHost.isBlank()) {
                return true;
            }
            // Comprobar socket por defecto de Docker
            return java.nio.file.Files.exists(java.nio.file.Path.of("/var/run/docker.sock"))
                || java.nio.file.Files.exists(java.nio.file.Path.of(
                        System.getProperty("user.home"), ".docker", "desktop", "docker.sock"))
                || java.nio.file.Files.exists(java.nio.file.Path.of(
                        System.getProperty("user.home"), ".colima", "docker.sock"));
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isRunning() {
        return CONTAINER != null && CONTAINER.isRunning();
    }

    public static String getJdbcUrl() {
        return CONTAINER.getJdbcUrl();
    }

    public static String getUsername() {
        return CONTAINER.getUsername();
    }

    public static String getPassword() {
        return CONTAINER.getPassword();
    }

    /**
     * ApplicationContextInitializer que registra las propiedades dinámicas
     * para usar el contenedor.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            if (!isRunning()) {
                return;
            }
            TestPropertyValues.of(
                    "spring.datasource.url=" + getJdbcUrl(),
                    "spring.datasource.username=" + getUsername(),
                    "spring.datasource.password=" + getPassword(),
                    "spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                    "spring.flyway.enabled=true",
                    "spring.flyway.baseline-on-migrate=true",
                    "spring.jpa.hibernate.ddl-auto=validate",
                    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect"
            ).applyTo(ctx.getEnvironment());
        }
    }
}
