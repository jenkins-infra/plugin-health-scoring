/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jenkins Infra
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

package io.jenkins.pluginhealth.scoring.model;

import java.util.List;

import io.jenkins.pluginhealth.scoring.scores.ScoringComponent;

/**
 * Describes the evaluation from a {@link ScoringComponent} on a specific plugin.
 *
 * @param score       the score representing the points granted to the plugin, out of 100 (one hundred).
 * @param weight      the weight of the score
 * @param reasons     the list of string explaining the score granted to the plugin
 * @param resolutions the list of link providing help to increase the score
 */
public record ScoringComponentResult(int score, float weight, List<String> reasons, List<String> resolutions) {
    public ScoringComponentResult(int score, float weight, List<String> reasons) {
        this(score, weight, reasons, List.of());
    }
}
