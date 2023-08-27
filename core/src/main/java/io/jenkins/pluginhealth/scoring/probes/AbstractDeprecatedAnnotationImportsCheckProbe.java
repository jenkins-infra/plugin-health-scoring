package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

/**
 * The abstract probe checks for deprecated imports in a plugin.
 */
public abstract class AbstractDeprecatedAnnotationImportsCheckProbe extends Probe {
    public static final int ORDER = SCMLinkValidationProbe.ORDER;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDeprecatedAnnotationImportsCheckProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();

        /* The "maxDepth" is set to Integer.MAX_VALUE because a repository can have multiple modules with class files in it. We do not want to miss any ".java" file.  */
        try (Stream<Path> paths = Files.find(scmRepository, Integer.MAX_VALUE, (path, $) ->
            Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))) {
            Set<String> deprecatedPaths = paths.map(this::getFileNameWithDeprecatedImport)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            return deprecatedPaths.isEmpty()
                ? ProbeResult.success(key(), String.format(getSuccessMessage() + " at %s plugin.", plugin.getName()))
                : ProbeResult.failure(key(), String.format(getFailureMessage() + " at %s plugin for ", plugin.getName()) + deprecatedPaths.stream().sorted(Comparator.naturalOrder()).collect(Collectors.joining(", ")));
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder during probe {}", key(), ex);
            return ProbeResult.error(key(), String.format("Could not browse the plugin folder during {} probe.", plugin.getName()));
        }
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY};
    }

    /**
     * Looks for all the deprecated imports in a path.
     *
     * @param path The file Path to check deprecated imports.
     * @return name of the file that contains deprecated imports.
     */
    private String getFileNameWithDeprecatedImport(Path path) {
        try (Stream<String> fileContent = Files.lines(path)) {
            if (CollectionUtils.containsAny(fileContent.toList(), getListOfDeprecatedImports())) {
                return path.getFileName().toString();
            }
        } catch (IOException ex) {
            LOGGER.error("Could not browse the {} plugin folder during probe. {}", key(), ex);
        }
        return null;
    }

    /**
     * @return a success message.
     */
    abstract String getSuccessMessage();

    /**
     * @return a failure message.
     */
    abstract String getFailureMessage();

    /**
     * @return a list of deprecated imports.
     */
    public abstract List<String> getListOfDeprecatedImports();
}
