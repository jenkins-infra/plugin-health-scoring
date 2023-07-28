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

package io.jenkins.pluginhealth.scoring.scores;

import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.DeprecatedPluginProbe;

import org.springframework.stereotype.Component;

@Component
public class DeprecatedPluginScoring extends Scoring {
    private static final float COEFFICIENT = .8f;
    private static final String KEY = "deprecation";

    @Override
    public ScoreResult apply(Plugin plugin) {
        final ProbeResult deprecatedPluginResult = plugin.getDetails().get(DeprecatedPluginProbe.KEY);
        if (deprecatedPluginResult == null) {
            return new ScoreResult(key(), 0, coefficient(), List.of());
        }
        return new ScoreResult(
            key(),
            "This plugin is marked as deprecated.".equals(deprecatedPluginResult.message()) ? 0 : 1,
            coefficient(),
            List.of(deprecatedPluginResult)
        );
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
        return "Scores plugin based on its deprecation status.";
    }
}
