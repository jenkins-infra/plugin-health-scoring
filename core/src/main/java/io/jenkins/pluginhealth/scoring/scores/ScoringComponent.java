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
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;

public interface ScoringComponent {
    /**
     * Provides a human readable description of the behavior of the Changelog.
     *
     * @return the description of the implementation.
     */
    String getDescription();

    /**
     * Evaluates the provided {@link ProbeResult} map against the requirement for this implementation
     * and returns a {@link ScoringComponentResult} describing the evaluation.
     *
     * @param plugin       the plugin to score
     * @param probeResults the plugin's {@link ProbeResult} map
     * @return a {@link ScoringComponentResult} describing the evaluation done based on the provided {@link ProbeResult} map
     */
    ScoringComponentResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults);

    /**
     * The weight of this Changelog
     *
     * @return an integer representing the importance of the current Changelog implementation.
     */
    int getWeight();
}
