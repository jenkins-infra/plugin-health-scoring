/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Jenkins Infra
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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class ProbeResultTest {
    @Test
    void shouldBeEqualsWithSameStatusAndIdAndMessage() {
        final ProbeResult result1 = new ProbeResult(
                "probe",
                "this is a message",
                ProbeResult.Status.SUCCESS,
                ZonedDateTime.now().minusDays(1),
                1);
        final ProbeResult result2 =
                new ProbeResult("probe", "this is a message", ProbeResult.Status.SUCCESS, ZonedDateTime.now(), 1);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void shouldNotBeEqualsWithDifferentMessages() {
        final ProbeResult result1 = new ProbeResult(
                "probe",
                "this is a message",
                ProbeResult.Status.SUCCESS,
                ZonedDateTime.now().minusDays(1),
                1);
        final ProbeResult result2 = new ProbeResult(
                "probe", "this is a different message", ProbeResult.Status.SUCCESS, ZonedDateTime.now(), 1);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void shouldNotBeEqualsWithDifferentStatues() {
        final ProbeResult result1 = new ProbeResult(
                "probe",
                "this is a message",
                ProbeResult.Status.SUCCESS,
                ZonedDateTime.now().minusDays(1),
                1);
        final ProbeResult result2 =
                new ProbeResult("probe", "this is a message", ProbeResult.Status.ERROR, ZonedDateTime.now(), 1);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void shouldNotBeEqualsWithDifferentMessagesAndStatues() {
        final ProbeResult result1 = new ProbeResult(
                "probe",
                "this is a message",
                ProbeResult.Status.SUCCESS,
                ZonedDateTime.now().minusDays(1),
                1);
        final ProbeResult result2 = new ProbeResult(
                "probe", "this is a different message", ProbeResult.Status.ERROR, ZonedDateTime.now(), 1);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void shouldNotBeEqualWithDifferentVersions() {
        final ProbeResult result1 =
                new ProbeResult("probe", "this is a message", ProbeResult.Status.SUCCESS, ZonedDateTime.now(), 1);
        final ProbeResult result2 =
                new ProbeResult("probe", "this is a message", ProbeResult.Status.SUCCESS, ZonedDateTime.now(), 2);

        assertThat(result1).isNotEqualTo(result2);
    }
}
