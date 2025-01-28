/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jenkins Infra
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
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe checks whether build failed on Default Branch or not.
 */
@Component
@Order(DefaultBranchBuildStatusProbe.ORDER)
public class DefaultBranchBuildStatusProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBranchBuildStatusProbe.class);

    public static final int ORDER = JenkinsCoreProbe.ORDER + 100;
    public static final String KEY = "default-branch-build-status";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        try {
            final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin ucPlugin =
                    context.getUpdateCenter().plugins().get(plugin.getName());
            if (ucPlugin == null) {
                return error("Plugin cannot be found in Update-Center.");
            }
            final String defaultBranch = ucPlugin.defaultBranch();
            if (defaultBranch == null || defaultBranch.isBlank()) {
                return this.error("No default branch configured for the plugin.");
            }
            final Optional<String> repositoryName = context.getRepositoryName();
            if (repositoryName.isEmpty()) {
                return this.error("No repository name configured for the plugin.");
            }
            final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
            final GHCommitStatus lastCommitStatus = ghRepository.getLastCommitStatus(defaultBranch);
            if (lastCommitStatus == null) {
                return error("There is no last commit status found for the plugin.");
            }
            return success(lastCommitStatus.getState().name());
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage(), ex);
            return this.error("Failed to obtain the status of the default branch.");
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Return whether the build is failed on default branch or not";
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
