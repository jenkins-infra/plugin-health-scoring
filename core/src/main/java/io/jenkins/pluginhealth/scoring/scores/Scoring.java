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

import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;

/**
 * Represents a scoring process of a plugin, based on ProbeResults contained within the Plugin#details map.
 */
public abstract class Scoring {
    /**
     * Starts the scoring process of the plugin.
     * By default, the method is using the {@link Scoring#getScoreComponents()} map to compute the score based on the
     * {@link Plugin#getDetails()} map. For each score component, if the probe is present in the plugin details and is
     * successful, the score get the maximum score for that component.
     *
     * @param plugin the plugin to score
     * @return a {@link ScoreResult} describing the plugin based on the ProbeResult and the scoring requirements.
     */
    public ScoreResult apply(Plugin plugin) {
        final Map<String, Float> scoreComponents = this.getScoreComponents();
        float max = 0;
        float score = 0;

        for (Map.Entry<String, Float> component : scoreComponents.entrySet()) {
            final float componentMaxValue = component.getValue();
            if (componentMaxValue > 0) {
                max += componentMaxValue;
            }
            final ProbeResult probeResult = plugin.getDetails().get(component.getKey());
            if (probeResult != null) {
                if (componentMaxValue > 0 && probeResult.status().equals(ProbeResult.Status.SUCCESS)) {
                    score += componentMaxValue;
                } else if (componentMaxValue < 0) {
                    score += componentMaxValue;
                }
            }
        }

        score = Math.round(Math.max(score, 0) / max * 100) / 100f;
        return new ScoreResult(key(), score, coefficient());
    }

    /**
     * Returns the key identifier for the scoring implementation.
     *
     * @return the identifier of the scoring implementation
     */
    public abstract String key();

    /**
     * Represents the weight of the scoring implementation applied to the overall plugin score.
     * Its value must be between 0 and 1 included.
     *
     * @return the weight of the scoring implementation.
     */
    public abstract float coefficient();

    /**
     * Returns a map describing the probe required for the score computation and their maximum score.
     * <br/>
     * The key is the probe key to consider.
     * The value is the value to be added to the score if the probe result is successful.
     * The value can be negative and in that case, the value will be considered in case the probe result is not
     *
     * @return map of Probe key to evaluate and their maximum score
     */
    public abstract Map<String, Float> getScoreComponents();

    /**
     * Returns a description of the scoring implementation.
     *
     * @return the description of the scoring implementation
     */
    public abstract String description();

    public String name() {
        return getClass().getSimpleName();
    }
}
