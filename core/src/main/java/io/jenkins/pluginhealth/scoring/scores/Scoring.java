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

import java.util.Set;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
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
        final float max = (float) this.getScoreComponents()
            .stream()
            .mapToDouble(ScoreComponent::weight)
            .sum();

        float score = (float) this.getScoreComponents()
            .stream()
            .mapToDouble(component -> component.score(plugin))
            .sum();

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
     * @return set of ScoreComponent to evaluate to score a plugin
     */
    public abstract Set<ScoreComponent> getScoreComponents();

    /**
     * Represents what should be validated as probe result in the plugin details
     *
     * @param operator the operator implementation to use to check the plugin details
     * @param weight   the weight given to ScoreComponent if the operator is validated
     */
    public record ScoreComponent(Operator operator, float weight) {
        public ScoreComponent {
            if (weight <= 0) {
                throw new IllegalArgumentException("The weight argument of ScoreComponent must be positive");
            }
        }

        public float score(Plugin plugin) {
            return operator.test(plugin) ? weight : 0;
        }
    }

    public sealed interface Operator permits And, Key {
        /**
         * Tests that the plugin is conforming to the Operator requirement.
         *
         * @param plugin the plugin to test
         * @return true if the plugin conforms to the operator requirement.
         */
        boolean test(Plugin plugin);


        /**
         * Provides a human readable form of the operator.
         *
         * @return the human readable form of the operator.
         */
        String display();
    }

    /**
     * Validates the present and the success of the specific Probe in the plugins details.
     *
     * @param key the key of the Probe to be present and successful in the plugin details.
     */
    public record Key(String key) implements Operator {
        @Override
        public boolean test(Plugin plugin) {
            final ProbeResult probeResult = plugin.getDetails().get(key);
            return probeResult != null && probeResult.status().equals(ResultStatus.SUCCESS);
        }

        @Override
        public String display() {
            return key;
        }
    }

    /**
     * Represents a boolean AND between two operators.
     * Both Operators have to be valid for this operator to be.
     *
     * @param operator1 the first operator of the expression
     * @param operator2 the second operator of the expression
     */
    public record And(Operator operator1, Operator operator2) implements Operator {
        @Override
        public boolean test(Plugin plugin) {
            return operator1.test(plugin) && operator2.test(plugin);
        }

        @Override
        public String display() {
            return "( " + operator1.display() + " && " + operator2.display() + " )";
        }
    }

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
