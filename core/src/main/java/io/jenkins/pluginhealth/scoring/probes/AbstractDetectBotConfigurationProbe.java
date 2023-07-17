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
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDetectBotConfigurationProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDetectBotConfigurationProbe.class);
    private final String botName;

    AbstractDetectBotConfigurationProbe(String botName) {
        this.botName = botName;
    }

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();
        final Path githubConfig = scmRepository.resolve(".github");
        if (Files.notExists(githubConfig)) {
            LOGGER.error("No GitHub configuration folder at {} ", key());
            return ProbeResult.failure(key(), "No GitHub configuration folder found");
        }

        try (Stream<Path> paths = Files.find(githubConfig, 1, (path, $) ->
         Files.isRegularFile(path) && path.getFileName().toString().startsWith(botName))) {
            return paths.findFirst()
                .map(file -> ProbeResult.success(key(), String.format("%s is configured", botName)))
                .orElseGet(() -> ProbeResult.failure(key(), String.format("No configuration file for %s", botName)));
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder during probe {}", key(), ex);
            return ProbeResult.error(key(), "Could not browse the plugin folder");
        }
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY};
    }
}
