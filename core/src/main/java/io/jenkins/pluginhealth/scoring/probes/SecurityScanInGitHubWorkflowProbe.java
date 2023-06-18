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

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = SCMLinkValidationProbe.ORDER)
public class SecurityScanInGitHubWorkflowProbe extends Probe {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityScanInGitHubWorkflowProbe.class);
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "security-scan-probe";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        return null;
    }

    @Override
    public String key() {
        return SecurityScanInGitHubWorkflowProbe.KEY;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY};
    }
}
