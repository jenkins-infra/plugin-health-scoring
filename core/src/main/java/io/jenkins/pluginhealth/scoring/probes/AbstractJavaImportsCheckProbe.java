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
import org.springframework.util.CollectionUtils;

/**
 * The abstract probe checks for imports in {@code .java} files in a plugin.
 */
public abstract class AbstractJavaImportsCheckProbe extends Probe {
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJavaImportsCheckProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();

        /* The "maxDepth" is set to Integer.MAX_VALUE because a repository can have multiple modules with class files in it. We do not want to miss any ".java" file.  */
        try (Stream<Path> javaFiles = Files.find(scmRepository, Integer.MAX_VALUE, (path, $) -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))) {
            Set<String> javaFilesWithDetectedImports = javaFiles.filter(javaFile -> containsImports(javaFile, getListOfImports()))
                .map(javaFile -> javaFile.getFileName().toString())
                .collect(Collectors.toSet());

            return javaFilesWithDetectedImports.isEmpty()
                ? ProbeResult.success(key(), String.format(getSuccessMessage() + " at %s plugin.", plugin.getName()))
                : ProbeResult.failure(key(), String.format(getFailureMessage() + " at %s plugin for ", plugin.getName()) + javaFilesWithDetectedImports.stream().sorted(Comparator.naturalOrder()).collect(Collectors.joining(", ")));
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder during {} probe. {}", key(), ex);
            return ProbeResult.error(key(), String.format("Could not browse the plugin folder during {} probe.", plugin.getName()));
        }
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY};
    }

    /**
     * Checks whether the file contains the required imports.
     *
     * @param path           The path of the file to check for imports.
     * @param importsToCheck The list of imports against which the lines in the file should be checked.
     * @return boolean {@code true} if the file contains all imports. {@code false} otherwise.
     */
    private boolean containsImports(Path path, List<String> importsToCheck) {
        try (Stream<String> importStatements = Files.lines(path).filter(line -> line.startsWith("import")).map(this::getFullyQualifiedImportName)) {
            return CollectionUtils.containsAny(importsToCheck, importStatements.toList());
        } catch (IOException ex) {
            LOGGER.error("Could not browse the {} plugin folder during probe. {}", key(), ex);
        }
        return false;
    }

    /**
     * @return a list of imports that should be checked.
     */
    abstract List<String> getListOfImports();

    /**
     * @return a success message.
     */
    abstract String getSuccessMessage();

    /**
     * @return a failure message.
     */
    abstract String getFailureMessage();

    /**
     * Gets a fully qualified name of the imported class/library from an {@code import} statement.
     *
     * @param importStatement the statement from which the fully qualified name should be fetched.
     * @return a String that contains only the fully qualified name of the class/library.
     */
    private String getFullyQualifiedImportName(String importStatement) {
        return importStatement.replace("import ", "").replace(";", "").trim();
    }
}
