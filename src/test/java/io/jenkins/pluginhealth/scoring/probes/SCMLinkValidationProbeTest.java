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

package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;

import io.jenkins.pluginhealth.scoring.config.GithubConfiguration;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;

class SCMLinkValidationProbeTest {
    @Test
    public void shouldKeepScmAsKey() {
        assertThat(new SCMLinkValidationProbe(new GithubConfiguration("")).key()).isEqualTo("scm");
    }

    @Test
    public void shouldRequireRelease() {
        assertThat(new SCMLinkValidationProbe(new GithubConfiguration("")).requiresRelease()).isTrue();
    }

    @Test
    public void shouldNotAcceptNullNorEmptyScm() {
        final Plugin p1 = new Plugin("foo", null, ZonedDateTime.now());
        final Plugin p2 = new Plugin("bar", "", ZonedDateTime.now());
        final SCMLinkValidationProbe probe = new SCMLinkValidationProbe(new GithubConfiguration(""));

        final ProbeResult r1 = probe.apply(p1);
        final ProbeResult r2 = probe.apply(p2);

        assertThat(r1.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r2.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r1.message()).isEqualTo("No SCM link");
    }

    @Test
    public void shouldRecognizeGitHubUrl() {
        final Plugin p1 = new Plugin("foo", "this-is-not-correct", ZonedDateTime.now());
        final SCMLinkValidationProbe probe = new SCMLinkValidationProbe(new GithubConfiguration(""));

        final ProbeResult r1 = probe.apply(p1);

        assertThat(r1.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r1.message()).isEqualTo("SCM link is invalid");
    }
}
