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
 * This probe checks if continuous delivery (CD) workflow is configured in the GitHub Actions
 */
@Component
@Order(ContinuousDeliveryProbe.ORDER)
public class ContinuousDeliveryProbe extends AbstractGitHubWorkflowProbe {
    public static final int ORDER = AbstractGitHubWorkflowProbe.ORDER;
    public static final String KEY = "jep-229";
    private static final String CD_WORKFLOW_IDENTIFIER = "jenkins-infra/github-reusable-workflows/.github/workflows/maven-cd.yml";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks if JEP-229 (Continuous Delivery) has been activated on the plugin";
    }

    @Override
    public String getWorkflowDefinition() {
        return CD_WORKFLOW_IDENTIFIER;
    }

    @Override
    public String getFailureMessage() {
        return "Could not find JEP-229 workflow definition";
    }

    @Override
    public String getSuccessMessage() {
        return "JEP-229 workflow definition found";
    }

}
