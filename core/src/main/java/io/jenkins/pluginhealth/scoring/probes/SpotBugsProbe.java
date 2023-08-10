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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(SpotBugsProbe.ORDER)
public class SpotBugsProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotBugsProbe.class);
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "spotbugs";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin ucPlugin =
            context.getUpdateCenter().plugins().get(plugin.getName());
        final String defaultBranch = ucPlugin.defaultBranch();
        try {
            final Optional<String> repositoryName = context.getRepositoryName();
            if (repositoryName.isPresent()) {
                final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
                final List<GHCheckRun> ghCheckRuns =
                    ghRepository.getCheckRuns(defaultBranch, Map.of("check_name", "SpotBugs")).toList();
                if (ghCheckRuns.size() != 1) {
                    return ProbeResult.success(key(), "SpotBugs not found in build configuration.");
                } else {
                    return ProbeResult.success(key(), "SpotBugs found in build configuration.");
                }
            } else {
                return ProbeResult.error(key(), "Cannot determine plugin repository.");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not get SpotBugs check for {}", plugin.getName(), e);
            return ProbeResult.error(key(), "Could not get SpotBugs check.");
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks if SpotBugs is enabled in a plugin.";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }
}
