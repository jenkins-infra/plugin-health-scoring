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

package io.jenkins.pluginhealth.scoring.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.scores.Scoring;

import org.springframework.stereotype.Service;

@Service
public class ScoringService {
    private final List<Scoring> scoringList;

    public ScoringService(List<Scoring> scoringList) {
        this.scoringList = List.copyOf(scoringList);
    }

    public List<Scoring> getScoringList() {
        return scoringList;
    }

    public Optional<Scoring> get(String key) {
        return scoringList.stream().filter(scoring -> scoring.key().equals(key)).findFirst();
    }

    public Map<String, ScoreView> getScoringsView() {
        return getScoringList().stream()
            .map(scoring -> new ScoringService.ScoreView(
                scoring.key(), scoring.weight(), scoring.description()
            ))
            .collect(Collectors.toMap(ScoreView::key, s -> s));
    }

    public record ScoreView(String key, float coefficient, String description) {}
}
