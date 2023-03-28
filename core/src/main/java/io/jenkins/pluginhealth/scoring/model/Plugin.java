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

package io.jenkins.pluginhealth.scoring.model;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.jenkins.pluginhealth.scoring.config.VersionNumberType;

import com.vladmihalcea.hibernate.type.json.JsonType;
import hudson.util.VersionNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "plugins")
public class Plugin {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "name", unique = true)
    private String name;

    @Type(VersionNumberType.class)
    @Column(name = "version", nullable = false)
    private VersionNumber version;

    @Column(name = "scm")
    private String scm;

    @Column(name = "release_timestamp")
    private ZonedDateTime releaseTimestamp;

    @Column(columnDefinition = "jsonb")
    @Type(value = JsonType.class)
    private final Map<String, ProbeResult> details = new HashMap<>();

    public Plugin() {
    }

    public Plugin(String name, VersionNumber version, String scm, ZonedDateTime releaseTimestamp) {
        this.name = name;
        this.version = version;
        this.scm = scm;
        this.releaseTimestamp = releaseTimestamp;
    }

    public String getName() {
        return name;
    }

    public VersionNumber getVersion() {
        return version;
    }

    public Plugin setVersion(VersionNumber version) {
        this.version = version;
        return this;
    }

    public String getScm() {
        return scm;
    }

    public Plugin setScm(String scm) {
        this.scm = scm;
        return this;
    }

    public ZonedDateTime getReleaseTimestamp() {
        return releaseTimestamp;
    }

    public Plugin setReleaseTimestamp(ZonedDateTime releaseTimestamp) {
        this.releaseTimestamp = releaseTimestamp;
        return this;
    }

    public Map<String, ProbeResult> getDetails() {
        return Map.copyOf(details);
    }

    public Plugin addDetails(ProbeResult newProbeResult) {
        this.details.compute(newProbeResult.id(), (s, previousProbeResult) ->
            Objects.equals(previousProbeResult, newProbeResult) ? previousProbeResult : newProbeResult
        );
        return this;
    }

    public Plugin addDetails(Map<String, ProbeResult> details) {
        details.values().forEach(this::addDetails);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plugin plugin = (Plugin) o;
        return name.equals(plugin.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
