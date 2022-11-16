/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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

package io.jenkins.pluginhealth.scoring.scores;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.ContinuousDeploymentProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotProbe;
import io.jenkins.pluginhealth.scoring.probes.JenkinsfileProbe;

import org.springframework.stereotype.Component;

@Component
public class PluginMaintenanceScoring extends Scoring {
    private static final float COEFFICIENT = .5f;
    private static final String KEY = "repository-configuration";

    @Override
    protected ScoreResult doApply(Plugin plugin) {
        final ProbeResult jenkinsfileProbeResult = plugin.getDetails().get(JenkinsfileProbe.KEY);
        final ProbeResult dependabotProbeResult = plugin.getDetails().get(DependabotProbe.KEY);
        final ProbeResult cdProbeResult = plugin.getDetails().get(ContinuousDeploymentProbe.KEY);

        if (jenkinsfileProbeResult == null || jenkinsfileProbeResult.status().equals(ResultStatus.FAILURE)) {
            return new ScoreResult(KEY, 0, COEFFICIENT);
        }

        if (dependabotProbeResult == null || dependabotProbeResult.status().equals(ResultStatus.FAILURE)) {
            return new ScoreResult(KEY, .75f, COEFFICIENT);
        }

        if (cdProbeResult == null || cdProbeResult.status().equals(ResultStatus.FAILURE)) {
            return new ScoreResult(KEY, .9f, COEFFICIENT);
        }

        return new ScoreResult(KEY, 1, COEFFICIENT);
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public float coefficient() {
        return COEFFICIENT;
    }

    @Override
    public String description() {
        return "Scores plugin based on Jenkinsfile presence, dependabot and JEP-229 configuration.";
    }
}
