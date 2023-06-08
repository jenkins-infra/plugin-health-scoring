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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringTest {
    @Mock private Plugin plugin;

    @BeforeEach
    void setup() {
        when(plugin.getDetails()).thenReturn(Map.of(
            "foo", ProbeResult.success("foo", ""),
            "bar", ProbeResult.success("bar", ""),
            "failed", ProbeResult.failure("failed", "")
        ));
    }

    @Test
    void shouldKeyOperatorValidateProbeResult() {
        final Scoring.Key foo = new Scoring.Key("foo");
        final Scoring.Key notThisOne = new Scoring.Key("not-this-one");
        final Scoring.Key failed = new Scoring.Key("failed");

        assertThat(foo.test(plugin)).isTrue();
        assertThat(failed.test(plugin)).isFalse();
        assertThat(notThisOne.test(plugin)).isFalse();
    }

    @Test
    void shouldAndOperatorBeCorrectlyImplemented() {
        final Scoring.Key foo = new Scoring.Key("foo");
        final Scoring.Key bar = new Scoring.Key("bar");
        final Scoring.Key failed = new Scoring.Key("failed");

        final Scoring.And success = new Scoring.And(foo, bar);
        final Scoring.And failure1 = new Scoring.And(foo, failed);
        final Scoring.And failure2 = new Scoring.And(failed, bar);

        assertThat(success.test(plugin)).isTrue();
        assertThat(failure1.test(plugin)).isFalse();
        assertThat(failure2.test(plugin)).isFalse();
    }
}
