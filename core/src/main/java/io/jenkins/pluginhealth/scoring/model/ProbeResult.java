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
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Represents the result of one analyze performed by a {@link io.jenkins.pluginhealth.scoring.probes.Probe} implementation on a {@link Plugin}
 *
 * @param id      represent the ID of the {@link io.jenkins.pluginhealth.scoring.probes.Probe}
 * @param message represents a summary of the result
 * @param status  represents the state of the analyze performed
 */
public record ProbeResult(String id, String message, Status status, ZonedDateTime timestamp) {
    public ProbeResult(String id, String message, Status status) {
        this(id, message, status, ZonedDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProbeResult that = (ProbeResult) o;
        return id.equals(that.id) && message.equals(that.message) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status);
    }

    public static ProbeResult success(String id, String message) {
        return new ProbeResult(id, message, Status.SUCCESS);
    }

    public static ProbeResult error(String id, String message) {
        return new ProbeResult(id, message, Status.ERROR);
    }

    /*
     * To handle the probe status migration from 2.x.y to 3.x.y, let consider the FAILURE status as an ERROR to force
     * the execution of the probe.
     */
    public enum Status {
        SUCCESS, @JsonAlias("FAILURE") ERROR
    }
}

