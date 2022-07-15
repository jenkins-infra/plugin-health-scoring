package io.jenkins.pluginhealth.scoring;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;
import io.jenkins.pluginhealth.scoring.service.PluginService;

import static org.junit.Assert.assertEquals;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.mock;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PluginServiceTest.DockerPostgresDatasourceInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@RunWith(MockitoJUnitRunner.class)
public class PluginServiceTest {
    private final PluginRepository pluginRepository;
    private final PluginService pluginService;

    Plugin plugin = new Plugin("myPlugin", "https://github.com/jenkinsci/my-plugin", null);

    public PluginServiceTest(PluginRepository pluginRepository, PluginService pluginService) {
        this.pluginRepository = pluginRepository;
        this.pluginService = pluginService;
    }

    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new PostgreSQLContainer("postgres:14.1")
        .withDatabaseName("testdb")
        .withUsername("sa")
        .withPassword("sa");

    public static class DockerPostgresDatasourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRE_SQL_CONTAINER.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRE_SQL_CONTAINER.getUsername(),
                "spring.datasource.password=" + POSTGRE_SQL_CONTAINER.getPassword()
            ).applyTo(applicationContext);
        }
    }

    @Test
    public void myTest() {
        pluginService.saveOrUpdate(plugin);
        pluginService.saveOrUpdate(plugin);

        assertEquals(1, pluginRepository.findAllByName("myPlugin").size());
    }
}
