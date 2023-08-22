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
import java.util.Optional;
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

@Component
@Order(IncrementalBuildDetectionProbe.ORDER)
public class IncrementalBuildDetectionProbe extends Probe {
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "incremental-build-maven-configuration";
    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalBuildDetectionProbe.class);
    private static final String INCREMENTAL_TOOL = "io.jenkins.tools.incrementals";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();
        final Path mvnConfig = scmRepository.resolve(".mvn");
        if (Files.notExists(mvnConfig)) {
            LOGGER.error("No Maven configuration folder at {}.", key());
            return ProbeResult.failure(key(), "No Maven configuration folder found.");
        }

        try (Stream<Path> extensionsPaths = Files.find(mvnConfig, 1, (path, $) ->
            Files.isRegularFile(path) && path.getFileName().toString().startsWith("extensions.xml"))) {
            try (Stream<Path> configPaths = Files.find(mvnConfig, 1, (path, $) ->
                Files.isRegularFile(path) && path.getFileName().toString().startsWith("maven.config"))) {
                Optional<Path> firstExtensionPath = extensionsPaths.findFirst();
                Optional<Path> firstConfigPath = configPaths.findFirst();

                if (firstExtensionPath.isPresent()) {
                    return isExtensionsXMLConfigured(firstExtensionPath.get())
                        ? ProbeResult.success(key(), "Incremental Build is configured in the plugin.")
                        : ProbeResult.failure(key(), "Incremental Build is not configured in the plugin.");
                } else if (firstConfigPath.isPresent()) {
                    return isMavenConfigConfigured(firstConfigPath.get())
                        ? ProbeResult.success(key(), "Incremental Build is configured in the plugin.")
                        : ProbeResult.failure(key(), "Incremental Build is not configured in the plugin.");
                } else {
                    return ProbeResult.failure(key(), "Incremental Build is not configured in the plugin.");
                }
            } catch (IOException ex) {
                LOGGER.error("Could not browse the plugin folder during probe {}. {}", key(), ex);
                return ProbeResult.error(key(), "Could not browse the plugin folder.");
            }
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder during probe {}. {}", key(), ex);
            return ProbeResult.error(key(), "Could not browse the plugin folder.");
        }
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

    private boolean isExtensionsXMLConfigured(Path path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(path.toFile());
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();
            Element extensionElement = (Element) rootElement.getElementsByTagName("extension").item(0);
            Element groupIdElement = (Element) extensionElement.getElementsByTagName("groupId").item(0);
            String groupId = groupIdElement.getTextContent();
            return groupId.equals(INCREMENTAL_TOOL);
        } catch (IOException e) {
            LOGGER.error("Could not read the file during probe {}. {}", key(), e);
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.error("Could not parse the file during probe {}. {}", key(), e);
        }
        return false;
    }

    private boolean isMavenConfigConfigured(Path path) {
        try {
            return Files.lines(path).anyMatch(line -> line.contains("-Pconsume-incrementals") || line.contains("-Pmight-produce-incrementals"));
        } catch (IOException e) {
            LOGGER.error("Could not read the file during probe {}. {}", key(), e);
        }
        return false;
    }
}
