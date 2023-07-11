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

public abstract class AbstractDetectBotConfigurationProbe extends Probe{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDetectBotConfigurationProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();
        final Path githubConfig = scmRepository.resolve(".github");
        if (Files.notExists(githubConfig)) {
            LOGGER.error("No GitHub configuration folder at {} ", key());
            return ProbeResult.failure(key(), "No GitHub configuration folder");
        }

        try (Stream<Path> paths = Files
            .find(githubConfig, 1, (path, basicFileAttributes) -> Files.isRegularFile(path)
                && path.getFileName().toString().startsWith(getBotToDetect()))) {
            return paths.findFirst()
                .map(file -> ProbeResult.success(key(), getSuccessMessage()))
                .orElseGet(() -> ProbeResult.failure(key(), getFailureMessage()));
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder at {} on {} ", key(), ex);
            return ProbeResult.error(key(), "Could not browse the plugin folder");
        }
    }

    @Override
    public String getDescription() {
        return "Abstract Probe to detect the bot configuration made in a plugin";
    }

    /**
     * @return the name of the bot to verify the configuration
     * */
    public abstract String getBotToDetect();

    public abstract String getSuccessMessage();

    public abstract String getFailureMessage();

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY};
    }

}
