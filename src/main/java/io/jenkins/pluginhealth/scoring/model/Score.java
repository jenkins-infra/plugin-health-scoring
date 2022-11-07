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

package io.jenkins.pluginhealth.scoring.model;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "scores")
@TypeDef(name = "json", typeClass = JsonType.class)
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plugin_id", updatable = false)
    private Plugin plugin;

    @Column(name = "computed_at")
    private ZonedDateTime computedAt;

    private transient long value = -1;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private final Set<ScoreResult> details = new HashSet<>();

    public Score() {
    }

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
        if (value == -1) {
            computeValue();
        }
        return value;
    }

    private void computeValue() {
        var sum = details.stream()
            .flatMapToDouble(res -> DoubleStream.of(res.value() * res.coefficient()))
            .sum();
        var coefficient = details.stream()
            .flatMapToDouble(res -> DoubleStream.of(res.coefficient()))
            .sum();
        this.value = Math.round(100 * (sum / coefficient));
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
