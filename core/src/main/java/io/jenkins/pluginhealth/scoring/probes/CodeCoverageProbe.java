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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(CodeCoverageProbe.ORDER)
public class CodeCoverageProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeCoverageProbe.class);
    private static final int LINE_COVERAGE_THRESHOLD = 75;
    private static final int BRANCH_COVERAGE_THRESHOLD = 75;

    private static final String COVERAGE_TITLE_REGEXP =
        "^Line: (?<line>\\d+(?:\\.\\d+)?)%(?: \\(.+\\))?. Branch: (?<branch>\\d+(?:\\.\\d+)?)%(?: \\(.+\\))?.$";
    private static final Pattern COVERAGE_TITLE_PATTERN = Pattern.compile(COVERAGE_TITLE_REGEXP);

    public static final String KEY = "code-coverage";
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin ucPlugin =
            context.getUpdateCenter().plugins().get(plugin.getName());
        final String defaultBranch = ucPlugin.defaultBranch();
        try {
            final Optional<String> repositoryName = context.getRepositoryName(plugin.getScm());
            if (repositoryName.isPresent()) {
                final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
                final List<GHCheckRun> ghCheckRuns =
                    ghRepository.getCheckRuns(defaultBranch, Map.of("check_name", "Code Coverage")).toList();
                if (ghCheckRuns.size() == 0) {
                    return ProbeResult.error(key(), "Could not determine code coverage for plugin");
                }

                double overall_line_coverage = 100;
                double overall_branch_coverage = 100;
                for (GHCheckRun checkRun : ghCheckRuns) {
                    final Matcher matcher = COVERAGE_TITLE_PATTERN.matcher(checkRun.getOutput().getTitle());
                    if (matcher.matches()) {
                        final double line_coverage = Double.parseDouble(matcher.group("line"));
                        final double branch_coverage = Double.parseDouble(matcher.group("branch"));
                        overall_line_coverage = Math.min(overall_line_coverage, line_coverage);
                        overall_branch_coverage = Math.min(overall_branch_coverage, branch_coverage);
                    }
                }
                return overall_line_coverage >= LINE_COVERAGE_THRESHOLD && overall_branch_coverage >= BRANCH_COVERAGE_THRESHOLD ?
                    ProbeResult.success(key(), "Line coverage is above " + LINE_COVERAGE_THRESHOLD + "%. Branch coverage is above " + BRANCH_COVERAGE_THRESHOLD + "%.") :
                    ProbeResult.failure(key(),
                        "Line coverage is " + (overall_line_coverage < LINE_COVERAGE_THRESHOLD ? "below " : "above ") + LINE_COVERAGE_THRESHOLD + "%. " +
                            "Branch coverage is " + (overall_branch_coverage < BRANCH_COVERAGE_THRESHOLD ? "below " : "above ") + BRANCH_COVERAGE_THRESHOLD + "%."
                    );
            } else {
                return ProbeResult.error(key(), "Cannot determine plugin repository");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not get Coverage check for {}", plugin.getName(), e);
            return ProbeResult.error(key(), "Could not get coverage check");
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Retrieve plugin code coverage details";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    @Override
    protected String[] getProbeResultRequirement() {
        return new String[]{
            SCMLinkValidationProbe.KEY,
            JenkinsfileProbe.KEY,
            UpdateCenterPluginPublicationProbe.KEY,
            LastCommitDateProbe.KEY,
        };
    }
}
