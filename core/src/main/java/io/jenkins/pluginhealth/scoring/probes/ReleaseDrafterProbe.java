/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jenkins Infra
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
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(ReleaseDrafterProbe.ORDER)
public class ReleaseDrafterProbe extends Probe {
    public static final String KEY = "release-drafter";
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseDrafterProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return this.error("There is no local repository for plugin " + plugin.getName() + ".");
        }
        final Path scmRepository = context.getScmRepository().get();
        final Path githubConfig = scmRepository.resolve(".github");
        if (Files.notExists(githubConfig)) {
            LOGGER.trace("No GitHub configuration folder at {} ", key());
            return this.success("No GitHub configuration folder found.");
        }

        try (Stream<Path> paths = Files.find(
                githubConfig,
                1,
                (path, $) -> Files.isRegularFile(path)
                        && isPathDrafterConfigFile((path.getFileName().toString())))) {
            return paths.findFirst()
                    .map(file -> this.success("Release Drafter is configured."))
                    .orElseGet(() -> this.success("Release Drafter is not configured."));
        } catch (IOException ex) {
            LOGGER.error("Could not browse {} for plugin {}", scmRepository.toAbsolutePath(), plugin.getName(), ex);
            return this.error("Could not browse the plugin folder.");
        }
    }

    private boolean isPathDrafterConfigFile(String filename) {
        return "release-drafter.yml".equals(filename) || "release-drafter.yaml".equals(filename);
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Check if Release Drafter is configured on a plugin or not";
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
