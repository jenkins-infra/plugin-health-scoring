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
import static org.mockito.Mockito.spy;

import java.net.http.HttpClient;
import java.util.List;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.config.GithubConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

@ExtendWith(MockitoExtension.class)
public class ProbesDescriptionValidationTest {
    @MockBean private static HttpClient httpClient;
    @MockBean private static GithubConfiguration githubConfiguration;

    private static Stream<Arguments> probes() {
        return Stream.of(
            new DeprecatedPluginProbe(),
            new LastCommitDateProbe(),
            new SCMLinkValidationProbe(httpClient, githubConfiguration),
            new UpForAdoptionProbe()
        ).map(probe -> Arguments.of(probe.getClass().getSimpleName(), probe));
    }

    @DisplayName(value = "Probes should have a custom description")
    @ParameterizedTest(name = "{0} description validation")
    @MethodSource(value = "probes")
    public void shouldValidateAllProbeImplementationsHaveProperDescription(String probeClassName, Probe probe) {
        final Probe unexpectedProbe = spy(Probe.class);
        assertThat(probe.getDescription()).isNotEqualTo(unexpectedProbe.getDescription());
    }
}
