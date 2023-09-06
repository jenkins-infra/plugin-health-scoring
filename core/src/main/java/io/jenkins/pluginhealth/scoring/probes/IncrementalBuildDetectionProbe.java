/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This probe checks for Incremental Build configuration in plugins.
 * It looks for configuration in {@code .mvn/maven.config} and {@code .mvn/extensions.xml} files.
 * The probe is successful when the configurations are found in both the files. Otherwise, it fails.
 */
@Component
@Order(IncrementalBuildDetectionProbe.ORDER)
public class IncrementalBuildDetectionProbe extends Probe {
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "incremental-build-maven-configuration";
    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalBuildDetectionProbe.class);
    private static final String INCREMENTAL_TOOL = "io.jenkins.tools.incrementals";
    private static final String INCREMENTAL_TOOL_ARTIFACT_ID = "git-changelist-maven-extension";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();
        final Path mvnConfig = scmRepository.resolve(".mvn");
        if (Files.notExists(mvnConfig)) {
            LOGGER.info("Could not find Maven configuration folder {} plugin while running {} probe.", plugin.getName(), key());
            return ProbeResult.failure(key(), String.format("Could not find Maven configuration folder for the %s plugin.", plugin.getName()));
        }

        try (Stream<Path> incrementalConfigsStream = Files.find(mvnConfig, 1, (path, $) -> Files.isRegularFile(path) && (path.endsWith("maven.config") || path.endsWith("extensions.xml")))) {
            List<Path> incrementalConfigsStore = incrementalConfigsStream.toList();

            // Here, we stored the data in a `List`, and we can create a supplier that will create a new `Stream` for us from this data
            Supplier<Stream<Path>> incrementalConfigs = incrementalConfigsStore::stream;

            Optional<Path> mavenExtensionsFile = incrementalConfigs.get().filter(path -> path.endsWith("extensions.xml")).findFirst();
            Optional<Path> mavenConfigFile = incrementalConfigs.get().filter(path -> path.endsWith("maven.config")).findFirst();

            if (mavenExtensionsFile.isPresent() && mavenConfigFile.isPresent()) {
                return isExtensionsXMLConfigured(mavenExtensionsFile.get()) && isMavenConfigConfigured(mavenConfigFile.get())
                    ? ProbeResult.success(key(), String.format("Incremental Build is configured in the %s plugin.", plugin.getName()))
                    : ProbeResult.failure(key(), String.format("Incremental Build is not configured in the %s plugin.", plugin.getName()));
            }
        } catch (IOException e) {
            LOGGER.error("Could not read files from .mvn directory for {} plugin while running {} probe.", plugin.getName(), key());
        }
        return ProbeResult.failure(key(), String.format("Incremental Build is not configured in the %s plugin.", plugin.getName()));
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{ContinuousDeliveryProbe.KEY};
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "The probe detects whether incremental build is configured in Maven Config.";
    }

    /**
     * Checks whether `extensions.xml` is configured in the plugin
     *
     * @param path Looks for extensions.xml configuration in the particular path
     * @return true if a correct configuration is found, otherwise false
     */
    private boolean isExtensionsXMLConfigured(Path path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(path.toFile());
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();
            Element extensionElement = (Element) rootElement.getElementsByTagName("extension").item(0);
            Element groupIdElement = (Element) extensionElement.getElementsByTagName("groupId").item(0);
            Element artifactIdElement = (Element) extensionElement.getElementsByTagName("artifactId").item(0);
            String groupId = groupIdElement.getTextContent();
            String artifactId = artifactIdElement.getTextContent();
            return INCREMENTAL_TOOL.equals(groupId) && INCREMENTAL_TOOL_ARTIFACT_ID.equals(artifactId);
        } catch (IOException e) {
            LOGGER.error("Could not read the file during probe {}. {}", key(), e);
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.error("Could not parse the file during probe {}. {}", key(), e);
        }
        return false;
    }

    /**
     * Checks whether `maven.config` is configured in the plugin
     *
     * @param path Looks for extensions.xml configuration in the particular path
     * @return true if a correct configuration is found, otherwise false
     */
    private boolean isMavenConfigConfigured(Path path) {
        try {
            return Files.readAllLines(path).containsAll(List.of("-Pconsume-incrementals", "-Pmight-produce-incrementals"));
        } catch (IOException e) {
            LOGGER.error("Could not read the file during probe {}. {}", key(), e);
        }
        return false;
    }
}
