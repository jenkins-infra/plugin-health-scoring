package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(DocumentationLocationProbe.ORDER)
public class DocumentationLocationProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationLocationProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repository = context.getScmRepository();
        if (Files.notExists(repository)) {
            return ProbeResult.error(key(), "Cannot find plugin repository");
        }

        try (Stream<Path> buildConfigs = getFiles(repository, "pom.xml", "build.gradle");
             Stream<Path> readmes = getFiles(repository, "README.md", "README.adoc")) {
            final Optional<Path> buildConfig = buildConfigs.findFirst();
            final Optional<Path> readme = readmes.findFirst();
            final String scm = plugin.getScm();

            if (readme.isEmpty()) {
                return ProbeResult.failure(key(), "The plugin has no README");
            }
            if (buildConfig.isEmpty()) {
                return ProbeResult.failure(key(), "Could not find plugin build configuration file");
            }

            return scm.equals(getBuildConfigSCM(buildConfig.get())) ?
                ProbeResult.success(key(), "The plugin documentation was migrated") :
                ProbeResult.failure(key(), "The plugin documentation was not migrated");
        } catch (IOException e) {
            LOGGER.warn("Problem inspecting plugin repository", e);
            return ProbeResult.failure(key(), "Cannot inspect plugin repository");
        }
    }

    private Stream<Path> getFiles(Path repository, String... fileNames) throws IOException {
        return Files.find(repository, 1, (path, basicFileAttributes) ->
            Files.isReadable(path) && (Arrays.stream(fileNames).anyMatch(name -> name.equals(path.getFileName().toString()))));
    }

    // TODO how to get the `project.url` in Maven or `jenkinsPlugin.url` in Gradle?
    private String getBuildConfigSCM(Path buildConfig) {
        return "";
    }

    @Override
    public String key() {
        return "documentation";
    }

    @Override
    protected boolean requiresRelease() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Checks if the plugin documentation was migrated to GitHub.";
    }
}
