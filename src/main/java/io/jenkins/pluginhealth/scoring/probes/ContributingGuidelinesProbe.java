package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
@Order(value = ContributingGuidelinesProbe.ORDER)
public class ContributingGuidelinesProbe extends Probe{
    private static final Logger LOGGER = LoggerFactory.getLogger(ContributingGuidelinesProbe.class);
    public static final int ORDER = JenkinsfileProbe.ORDER + 1;
    private static final String KEY = "contributing-guidelines";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (plugin.getDetails().get(SCMLinkValidationProbe.KEY) == null) {
            LOGGER.error("Couldn't run {} on {} because previous SCMLinkValidationProbe has null value in database", key(), plugin.getName());
            return ProbeResult.error(key(), "SCM link has not been validated yet");
        }
        final Path repository = context.getScmRepository();
        try (Stream<Path> paths = Files
            .find(repository, 4, (file, basicFileAttributes) -> Files.isReadable(file) && ("CONTRIBUTING.md".equalsIgnoreCase(file.getFileName().toString()) || "CONTRIBUTING.adoc".equalsIgnoreCase(file.getFileName().toString())))
        ) {
            return paths.findFirst()
                .map(file -> ProbeResult.success(key(), "Contributing guidelines found"))
                .orElseGet(() -> ProbeResult.failure(key(), "No contributing guidelines found"));
        } catch (IOException e) {
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return """
            Validates the existence of a `CONTRIBUTING.adoc` or `CONTRIBUTING.md` file in the repository.
            """;
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }
}
