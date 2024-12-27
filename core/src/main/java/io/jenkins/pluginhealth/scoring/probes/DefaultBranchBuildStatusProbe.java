/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jenkins Infra
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

import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe checks whether build failed on Default Branch or not.
 */
@Component
@Order(DefaultBranchBuildStatusProbe.ORDER)
public class DefaultBranchBuildStatusProbe extends Probe {

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
            final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
            GHCommit commit = ghRepository.getCommit(ucPlugin.defaultBranch());
            GHCheckRun checkRun = commit.getCheckRuns().iterator().next();
            Conclusion conclusion = checkRun.getConclusion();

            if (conclusion == Conclusion.FAILURE) {
                return this.success("Build failed in default branch");
            } else {
                return this.success("Build is successful in default branch");
            }
        } catch (Exception ex) {
            return this.error("Failed to obtain the status of the default Branch.");
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
