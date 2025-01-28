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
package io.jenkins.pluginhealth.scoring.scores;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a scoring process of a plugin, based on ProbeResults contained within the Plugin#details map.
 */
public abstract class Scoring {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scoring.class);

    /**
     * Starts the scoring process of the plugin.
     * At the end of the process, a {@link ScoreResult} instance must be returned, describing the score of the plugin and its reasons.
     *
     * @param plugin the plugin to score
     * @return a {@link ScoreResult} describing the plugin based on the ProbeResult and the scoring requirements.
     */
    public final ScoreResult apply(Plugin plugin) {
        return getComponents().stream()
                .map(changelog -> {
                    try {
                        return changelog.getScore(plugin, plugin.getDetails());
                    } catch (Throwable e) {
                        LOGGER.warn(
                                "Problem running {} on {} because of {}",
                                this.getClass().getCanonicalName(),
                                plugin.getName(),
                                e.getClass().getCanonicalName(),
                                e);
                        return new ScoringComponentResult(
                                0,
                                changelog.getWeight(),
                                List.of("Could not run scoring because of "
                                        + e.getClass().getCanonicalName()));
                    }
                })
                .collect(new Collector<ScoringComponentResult, Set<ScoringComponentResult>, ScoreResult>() {
                    @Override
                    public Supplier<Set<ScoringComponentResult>> supplier() {
                        return HashSet::new;
                    }

                    @Override
                    public BiConsumer<Set<ScoringComponentResult>, ScoringComponentResult> accumulator() {
                        return Set::add;
                    }

                    @Override
                    public BinaryOperator<Set<ScoringComponentResult>> combiner() {
                        return (changelogResults, changelogResults2) -> {
                            changelogResults.addAll(changelogResults2);
                            return changelogResults;
                        };
                    }

                    @Override
                    public Function<Set<ScoringComponentResult>, ScoreResult> finisher() {
                        return changelogResults -> {
                            final double sum = changelogResults.stream()
                                    .flatMapToDouble(changelogResult ->
                                            DoubleStream.of(changelogResult.score() * changelogResult.weight()))
                                    .sum();
                            final double weight = changelogResults.stream()
                                    .flatMapToDouble(changelogResult -> DoubleStream.of(changelogResult.weight()))
                                    .sum();
                            return new ScoreResult(
                                    key(),
                                    weight == 0 ? 100 : (int) Math.max(0, Math.round(sum / weight)),
                                    weight(),
                                    changelogResults,
                                    version());
                        };
                    }

                    @Override
                    public Set<Characteristics> characteristics() {
                        return Set.of(Characteristics.UNORDERED);
                    }
                });
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
    public abstract float weight();

    /**
     * Returns a description of the scoring implementation.
     *
     * @return the description of the scoring implementation
     */
    public abstract String description();

    /**
     * Provides the list of elements evaluated for this scoring.
     *
     * @return the list of {@link ScoringComponent} considered for this score category of a plugin.
     */
    public abstract List<ScoringComponent> getComponents();

    public final String name() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the version of the scoring implementation.
     * When a scoring implementation is changed, this needs to be updated.
     *
     * @return an integer representing the scoring implementation version.
     */
    public abstract int version();
}
