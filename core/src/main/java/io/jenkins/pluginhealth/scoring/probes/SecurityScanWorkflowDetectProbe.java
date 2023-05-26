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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = SecurityScanWorkflowDetectProbe.ORDER)
public class SecurityScanWorkflowDetectProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "security-scan";

    private static final String SEARCH_LINE = "uses: jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml@v2";
    private static final String WORKFLOWS_DIRECTORY = ".github/workflows";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repository = context.getScmRepository();
        final Path workflowsPath = Paths.get(repository.toString(), WORKFLOWS_DIRECTORY);
        try (Stream<Path> paths = Files.walk(workflowsPath)) {
            List<String> fileNames = new ArrayList<>();
            paths.filter(Files::isRegularFile)
                .filter(this::containsSearchLine)
                .forEach(path -> fileNames.add(path.toString()));
            if (!fileNames.isEmpty()) {
                String fileList = String.join(", ", fileNames);
                return ProbeResult.success(key(), "The line is present in the following files: " + fileList);
            } else {
                return ProbeResult.failure(key(), "The line is not present in any file in the repository");
            }
        } catch (IOException e) {
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    private boolean containsSearchLine(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.anyMatch(line -> line.contains(SEARCH_LINE));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks for the presence of the line 'uses: jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml' in the repository files located in the .github/workflows directory.";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[] { SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY };
    }
}
