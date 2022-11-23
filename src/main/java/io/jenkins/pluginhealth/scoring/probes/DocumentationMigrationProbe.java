/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(DocumentationMigrationProbe.ORDER)
public class DocumentationMigrationProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationMigrationProbe.class);
    private static final String GRADLE_PLUGINS_LISTING = "/probes/DocumentationMigrationProbe/gradle-migrated-plugins.txt";

    public static final int ORDER = LastCommitDateProbe.ORDER + 1;
    public static final String KEY = "documentation";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repository = context.getScmRepository();
        final ProbeResult scmLinkValidationProbe = plugin.getDetails().get(SCMLinkValidationProbe.KEY);
        if (scmLinkValidationProbe == null || scmLinkValidationProbe.status().equals(ResultStatus.FAILURE)) {
            return ProbeResult.error(
                key(),
                "Cannot validate documentation for %s because the scm link was not validated".formatted(plugin.getName())
            );
        }
        if (Files.notExists(repository)) {
            return ProbeResult.error(key(), "Cannot find plugin repository");
        }

        try (Stream<Path> buildConfigs = getFiles(repository, "pom.xml", "build.gradle", "build.gradle.kts");
             Stream<Path> readmes = getFiles(repository, "README.md", "README.adoc")) {
            final Optional<Path> buildConfig = buildConfigs.findFirst();
            final Optional<Path> readme = readmes.findFirst();

            if (readme.isEmpty()) {
                return ProbeResult.failure(key(), "The plugin has no README");
            }
            if (buildConfig.isEmpty()) {
                return ProbeResult.failure(key(), "Could not find plugin build configuration file");
            }

            return isCorrectlyConfigured(plugin, buildConfig.get()) ?
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

    private boolean isCorrectlyConfigured(Plugin plugin, Path buildConfig) {
        final Path fileName = buildConfig.getFileName();
        if (fileName == null) {
            return false;
        }
        if ("pom.xml".equals(fileName.toString())) {
            final String urlFromMaven = getURLFromMaven(buildConfig);
            return urlFromMaven != null && urlFromMaven.startsWith(plugin.getScm());
        } else if (fileName.toString().startsWith("build.gradle")) {
            try (InputStream is = getClass().getResourceAsStream(GRADLE_PLUGINS_LISTING)) {
                if (is == null) {
                    return false;
                }
                try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                     BufferedReader br = new BufferedReader(isr)) {
                    return br.lines()
                        .filter(line -> line.equals(plugin.getName()))
                        .anyMatch(line -> plugin.getName().equals(line));
                }
            } catch (IOException e) {
                LOGGER.warn("Problem while testing {} for documentation migration", plugin.getName(), e);
                return false;
            }
        }
        LOGGER.warn("Unrecognizable build tool for {}", plugin.getName());
        return false;
    }

    private String getURLFromMaven(Path buildConfig) {
        try {
            final MavenXpp3Reader maven = new MavenXpp3Reader();
            final Model pom = maven.read(Files.newInputStream(buildConfig));
            return pom.getUrl();
        } catch (IOException | XmlPullParserException e) {
            LOGGER.warn("Error parsing Maven pom.xml in {}", buildConfig, e);
            return "";
        }
    }

    @Override
    public String key() {
        return KEY;
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
