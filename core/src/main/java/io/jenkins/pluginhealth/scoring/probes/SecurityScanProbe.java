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

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe checks if Jenkins-infra security scan is configured in the GitHub Actions
 */
@Component
@Order(SecurityScanProbe.ORDER)
public class SecurityScanProbe extends AbstractGitHubWorkflowProbe {
    public static final int ORDER = AbstractGitHubWorkflowProbe.ORDER;
    public static final String KEY = "security-scan";
    private static final String SECURITY_SCAN_WORKFLOW_IDENTIFIER = "jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks if Security Scan is configured in GitHub workflow";
    }

    @Override
    public String getWorkflowDefinition() {
        return SECURITY_SCAN_WORKFLOW_IDENTIFIER;
    }

    @Override
    public String getFailureMessage() {
        return "GitHub workflow security scan is not configured in the plugin.";
    }

    @Override
    public String getSuccessMessage() {
        return "GitHub workflow security scan is configured in the plugin.";
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
