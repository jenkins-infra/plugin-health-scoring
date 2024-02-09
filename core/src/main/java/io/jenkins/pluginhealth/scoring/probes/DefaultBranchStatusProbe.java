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

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe checks whether build failed on Default Branch or not.
 */
@Component
@Order(DefaultBranchStatusProbe.ORDER)
public class DefaultBranchStatusProbe extends Probe {

    public static final int ORDER = JenkinsCoreProbe.ORDER + 100;
    public static final String KEY = "default-branch-build-status";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getRepositoryName().isPresent()) {
            return this.success("There is no local repository for plugin " + plugin.getName() + ".");
        }
        try {

                final GitHub gh = context.getGitHub();
                final GHRepository repository = gh.getRepository(context.getRepositoryName().orElseThrow());
                String defaultBranch = repository.getDefaultBranch();
                GHCommit commit = repository.getCommit(defaultBranch);
                GHCheckRun checkRun = commit.getCheckRuns().iterator().next();
                GHCheckRun.Conclusion conclusion = checkRun.getConclusion();

                if (conclusion == GHCheckRun.Conclusion.FAILURE) {
                    return this.success("Build Failed in Default Branch");
                } else {
                    return this.success("Build is Success in Default Branch");
                }
            }
        catch (IOException e) {
            return this.error("Could not get failingBuilding Check");
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
