/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jenkins Infra
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Probe to detect the parent POM version used by a Jenkins plugin.
 *
 * <p>Jenkins plugins inherit build configuration from the parent POM
 * (https://github.com/jenkinsci/plugin-pom). This probe extracts the
 * parent POM version from the plugin's pom.xml to help track modernization
 * and identify plugins using outdated parent versions.</p>
 */
@Component
@Order(value = ParentPomVersionProbe.ORDER)
public class ParentPomVersionProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "parent-pom-version";

    // Regex to match: <parent>...<version>X.Y.Z</version>...</parent>
    private static final Pattern PARENT_VERSION_PATTERN =
            Pattern.compile("<parent>.*?<version>\\s*([^<]+?)\\s*</version>.*?</parent>", Pattern.DOTALL);

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Detects the parent POM version used by the plugin";
    }

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        // Check if we have a local repository
        if (context.getScmRepository().isEmpty()) {
            return this.error("There is no local repository for plugin " + plugin.getName() + ".");
        }

        final Path repository = context.getScmRepository().get();
        final Path pomFile = repository.resolve("pom.xml");

        // Check if pom.xml exists
        if (!Files.exists(pomFile)) {
            return this.error("pom.xml not found in repository");
        }

        try {
            // Read pom.xml content
            String pomContent = Files.readString(pomFile);

            // Extract parent version
            String parentVersion = extractParentVersion(pomContent);

            if (parentVersion == null) {
                return this.error("No parent POM found in pom.xml");
            }

            // Return success with the version
            return this.success(parentVersion);

        } catch (IOException e) {
            return this.error("Error reading pom.xml: " + e.getMessage());
        }
    }

    /**
     * Extracts the parent POM version from pom.xml content.
     *
     * @param pomContent the content of pom.xml
     * @return the parent version string, or null if not found
     */
    private String extractParentVersion(String pomContent) {
        Matcher matcher = PARENT_VERSION_PATTERN.matcher(pomContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    @Override
    protected boolean isSourceCodeRelated() {
        // Parent POM version is in the source code (pom.xml)
        // So this should return true to re-run when code changes
        return true;
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
