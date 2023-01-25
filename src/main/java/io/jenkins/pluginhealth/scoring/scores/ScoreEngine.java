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

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.ScoreService;
import io.jenkins.pluginhealth.scoring.service.ScoringService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class ScoreEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreEngine.class);

    private final ScoringService scoringService;
    private final PluginService pluginService;
    private final ScoreService scoreService;

    public ScoreEngine(ScoringService scoringService, PluginService pluginService, ScoreService scoreService) {
        this.scoringService = scoringService;
        this.pluginService = pluginService;
        this.scoreService = scoreService;
    }

    public void run() {
        LOGGER.info("Start scoring all plugins");
        pluginService.streamAll()
            .forEach(this::runOn);
        LOGGER.info("Score engine has finished");
    }

    public Score runOn(Plugin plugin) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Scoring {}", plugin.getName());
        }
        final Optional<ZonedDateTime> latestProbeResult = plugin.getDetails().values().stream()
            .map(ProbeResult::timestamp).max(Comparator.naturalOrder());
        final Optional<Score> latestScore = scoreService.latestScoreFor(plugin.getName());
        if (latestScore.isPresent() && (latestProbeResult.isEmpty() || latestProbeResult.get().isBefore(latestScore.get().getComputedAt()))) {
            LOGGER.debug("Previous score, computed at {} is still valid.", latestScore.get().getComputedAt());
            return latestScore.get();
        }

        Score score = this.scoringService.getScoringList().stream()
            .map(scoring -> scoring.apply(plugin))
            .collect(new Collector<ScoreResult, Score, Score>() {
                @Override
                public Supplier<Score> supplier() {
                    return () -> new Score(plugin, ZonedDateTime.now());
                }

                @Override
                public BiConsumer<Score, ScoreResult> accumulator() {
                    return Score::addDetail;
                }

                @Override
                public BinaryOperator<Score> combiner() {
                    return (score1, score2) -> {
                        for (ScoreResult res : score2.getDetails()) {
                            score1.addDetail(res);
                        }
                        return score1;
                    };
                }

                @Override
                public Function<Score, Score> finisher() {
                    return Function.identity();
                }

                @Override
                public Set<Characteristics> characteristics() {
                    return EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
                }
            });

        try {
            return scoreService.save(score);
        } catch (Throwable t) {
            LOGGER.error("Could not save the score for {}", score.getPlugin().getName(), t);
            return null;
        }
    }
}
