package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe checks for deprecated @Nonnull and @CheckForNull annotations in a plugin.
 */
@Component
@Order(value = JSR305Probe.ORDER)
public class JSR305Probe extends Probe {
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "JSR-305";
    private static final Logger LOGGER = LoggerFactory.getLogger(JSR305Probe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return ProbeResult.error(key(), "There is no local repository for plugin " + plugin.getName() + ".", this.getVersion());
        }
        final Path scmRepository = context.getScmRepository().get();

        /* The "maxDepth" is set to Integer.MAX_VALUE because a repository can have multiple modules with class files in it. We do not want to miss any ".java" file.  */
        try (Stream<Path> javaFiles = Files.find(scmRepository, Integer.MAX_VALUE, (path, $) -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))) {
            Set<String> javaFilesWithDetectedImports = javaFiles
                .filter(this::containsImports)
                .map(javaFile -> javaFile.getFileName().toString())
                .collect(Collectors.toSet());

            return javaFilesWithDetectedImports.isEmpty()
                ? ProbeResult.success(key(), String.format(getSuccessMessage() + " at %s plugin.", plugin.getName()), this.getVersion())
                : ProbeResult.success(key(), String.format(getFailureMessage() + " at %s plugin for ", plugin.getName()) + javaFilesWithDetectedImports.stream().sorted(Comparator.naturalOrder()).collect(Collectors.joining(", ")), this.getVersion());
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder during {} probe.", key(), ex);
            return ProbeResult.error(key(), String.format("Could not browse the plugin folder during %s probe.", plugin.getName()), this.getVersion());
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "The probe checks for deprecated annotations.";
    }

    /**
     * Fetches all the import for the given file.
     *
     * @param javaFile The file to read and fetch the import from.
     * @return a List with imports is returned when imports are found. Otherwise, an empty list is returned.
     */
    private List<String> getAllImportsInTheFile(Path javaFile) {
        try (Stream<String> importStatements = Files.lines(javaFile).filter(line -> line.startsWith("import")).map(this::getFullyQualifiedImportName)) {
            return importStatements.toList();
        } catch (IOException ex) {
            LOGGER.error("Could not browse the {} plugin folder during probe.", key(), ex);
        }
        return List.of();
    }

    /**
     * @return the import String that should be checked.
     */
    public String getImportToCheck() {
        return "javax.annotation";
    }

    /**
     * @return a success message.
     */
    String getSuccessMessage() {
        return "Latest version of imports found";
    }

    /**
     * @return a failure message.
     */
    String getFailureMessage() {
        return "Deprecated imports found";
    }

    /**
     * Gets a fully qualified name of the imported class/library from an {@code import} statement.
     *
     * @param importStatement the statement from which the fully qualified name should be fetched.
     * @return a String that contains only the fully qualified name of the class/library.
     */
    private String getFullyQualifiedImportName(String importStatement) {
        return importStatement.replace("import ", "").replace(";", "").trim();
    }

    /**
     * Checks whether the file contains the required imports.
     *
     * @param javaFile The file to fetch all the imports for.
     * @return boolean {@code true} if the file contains all imports. {@code false} otherwise.
     */
    private boolean containsImports(Path javaFile) {
        List<String> imports = getAllImportsInTheFile(javaFile);
        return imports.stream().anyMatch(line -> line.startsWith(getImportToCheck()));
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
