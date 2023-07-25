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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Analyzes the {@link Plugin#getScm()} value to determine if the link is valid or not.
 */
@Component
@Order(value = SCMLinkValidationProbe.ORDER)
public class SCMLinkValidationProbe extends Probe {
    public static final int ORDER = UpdateCenterPluginPublicationProbe.ORDER + 100;
    public static final String KEY = "scm";
    private static final Logger LOGGER = LoggerFactory.getLogger(SCMLinkValidationProbe.class);
    private static final String GH_REGEXP = "https://(?<server>[^/]*)/(?<repo>jenkinsci/[^/]*)";
    public static final Pattern GH_PATTERN = Pattern.compile(GH_REGEXP);

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (plugin.getScm() == null || plugin.getScm().isBlank()) {
            LOGGER.warn("{} has no SCM link", plugin.getName());
            return ProbeResult.error(key(), "The plugin SCM link is empty");
        }
        return fromSCMLink(context, plugin.getScm(), plugin.getName());
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return """
            The SCMLinkValidation probe validates with GitHub API \
            if the known SCM link of a plugin is correct or not.
            """;
    }

    @Override
    protected boolean requiresRelease() {
        return true;
    }

    private ProbeResult fromSCMLink(ProbeContext context, String scm, String pluginName) {
        Matcher matcher = GH_PATTERN.matcher(scm);
        if (!matcher.find()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is not respecting the SCM URL Template", scm);
            }
            return ProbeResult.failure(key(), "SCM link doesn't match GitHub plugin repositories");
        }

        try {
            context.getGitHub().getRepository(matcher.group("repo"));
            String repo = matcher.group("repo");
            String folderPath = searchPomFiles(Paths.get(repo), pluginName, scm);
            context.setScmFolderPath(folderPath);
            return ProbeResult.success(key(), "The plugin SCM link is valid");
        } catch (IOException ex) {
            return ProbeResult.failure(key(), "The plugin SCM link is invalid");
        }
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{UpdateCenterPluginPublicationProbe.KEY};
    }

    /**
     * Searches for Pom files in every directory available in the repository
     *
     * @param directory  in the scm
     * @param pluginName the name of the plugin
     * @param scm        the valid scm link
     * @return folderPath if it valid or return the scm itself
     */
    private String searchPomFiles(Path directory, String pluginName, String scm) {
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();

        try (Stream<Path> paths = Files.find(directory, 1, (path, $) ->
            path.getFileName().toString().equals("pom.xml")
        )) {
            try (Reader reader = new InputStreamReader(new FileInputStream((File) paths), StandardCharsets.UTF_8)) {
                Model model = mavenReader.read(reader);
                if (model.getPackaging().equals("hpi") && model.getArtifactId().equals(pluginName)) {
                    return directory.toString();
                }
            } catch (IOException e) {
                LOGGER.error("Pom file not found for {}", pluginName, e);
            } catch (XmlPullParserException e) {
                LOGGER.error("Could not parse pom file for {}", pluginName, e);
            }
        } catch (IOException e) {
            LOGGER.error("Could not browse the folder during probe {}", pluginName, e);
        }
        return scm;
    }
}
