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

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.repository.ScoreRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {
    private final ScoreRepository repository;
    private final PluginService pluginService;

    public ScoreService(ScoreRepository repository, PluginService pluginService) {
        this.repository = repository;
        this.pluginService = pluginService;
    }

    @Transactional
    public Score save(Score score) {
        return repository.save(score);
    }

    @Transactional(readOnly = true)
    public Optional<Score> latestScoreFor(String pluginName) {
        return pluginService.findByName(pluginName)
            .flatMap(repository::findFirstByPluginOrderByComputedAtDesc);
    }

    @Transactional(readOnly = true)
    public Map<String, ScoreSummary> getLatestScoresSummaryMap() {
        return repository.findLatestScoreForAllPlugins().stream()
            .collect(Collectors.toMap(
                score -> score.getPlugin().getName(),
                ScoreSummary::fromScore
            ));
    }

    public record ScoreSummary(long value, String version, Set<ScoreResult> details, ZonedDateTime timestamp) {
        public static ScoreSummary fromScore(Score score) {
            final Plugin plugin = score.getPlugin();
            return new ScoreSummary(
                score.getValue(),
                plugin.getVersion() == null ? "" : plugin.getVersion().toString(),
                score.getDetails(),
                score.getComputedAt()
            );
        }
    }
}
