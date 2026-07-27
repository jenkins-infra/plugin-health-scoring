/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jenkins Infra
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

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.DoubleStream;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "scores")
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plugin_id", updatable = false)
    private Plugin plugin;

    @Column(name = "computed_at")
    private ZonedDateTime computedAt;

    @Column(name = "value")
    private long value = 0;

    @Column(columnDefinition = "jsonb")
    @Type(JsonType.class)
    private final Set<ScoreResult> details = new HashSet<>();

    public Score() {}

    public Score(Plugin plugin, ZonedDateTime computedAt) {
        this.plugin = plugin;
        this.computedAt = computedAt;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public ZonedDateTime getComputedAt() {
        return computedAt;
    }

    public long getValue() {
        return value;
    }

    private void computeValue() {
        var sum = details.stream()
                .filter(Objects::nonNull)
                .flatMapToDouble(res -> DoubleStream.of(res.value() * res.weight()))
                .sum();
        var coefficient = details.stream()
                .filter(Objects::nonNull)
                .flatMapToDouble(res -> DoubleStream.of(res.weight()))
                .sum();
        this.value = Math.round((sum / coefficient));
    }

    public void addDetail(ScoreResult result) {
        this.details.add(result);
        computeValue();
    }

    public Set<ScoreResult> getDetails() {
        return Set.copyOf(details);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Score score = (Score) o;
        return Objects.equals(plugin, score.plugin) && Objects.equals(computedAt, score.computedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plugin, computedAt);
    }
}
