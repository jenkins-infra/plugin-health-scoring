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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

import io.jenkins.pluginhealth.scoring.config.GithubConfiguration;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SCMLinkValidationProbeTest {
    @Mock private HttpClient httpClient;
    @Mock private GithubConfiguration githubConfiguration;

    @Test
    public void shouldKeepScmAsKey() {
        assertThat(new SCMLinkValidationProbe(httpClient, githubConfiguration).key()).isEqualTo("scm");
    }

    @Test
    public void shouldRequireRelease() {
        assertThat(new SCMLinkValidationProbe(httpClient, githubConfiguration).requiresRelease()).isTrue();
    }

    @Test
    public void shouldNotAcceptNullNorEmptyScm() {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctxP1 = mock(ProbeContext.class);
        final Plugin p2 = mock(Plugin.class);
        final ProbeContext ctxP2 = mock(ProbeContext.class);
        final SCMLinkValidationProbe probe = new SCMLinkValidationProbe(httpClient, githubConfiguration);

        when(p1.getScm()).thenReturn(null);
        when(p2.getScm()).thenReturn("");

        final ProbeResult r1 = probe.apply(p1, ctxP1);
        final ProbeResult r2 = probe.apply(p2, ctxP2);

        assertThat(r1.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r2.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r1.message()).isEqualTo("The plugin SCM link is empty");
    }

    @Test
    public void shouldRecognizeIncorrectSCMUrl() {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final SCMLinkValidationProbe probe = new SCMLinkValidationProbe(httpClient, githubConfiguration);

        when(p1.getScm()).thenReturn("this-is-not-correct");
        final ProbeResult r1 = probe.apply(p1, ctx);

        assertThat(r1.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r1.message()).isEqualTo("SCM link doesn't match GitHub plugin repositories");
    }

    @Test
    public void shouldRecognizeCorrectGitHubUrl() throws IOException, InterruptedException {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final SCMLinkValidationProbe probe = new SCMLinkValidationProbe(httpClient, githubConfiguration);

        when(p1.getScm()).thenReturn("https://github.com/jenkinsci/mailer-plugin");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(
            new HttpResponse<>() {
                @Override
                public int statusCode() {
                    return 200;
                }

                @Override
                public HttpRequest request() {
                    return null;
                }

                @Override
                public Optional<HttpResponse<String>> previousResponse() {
                    return Optional.empty();
                }

                @Override
                public HttpHeaders headers() {
                    return null;
                }

                @Override
                public String body() {
                    return null;
                }

                @Override
                public Optional<SSLSession> sslSession() {
                    return Optional.empty();
                }

                @Override
                public URI uri() {
                    return null;
                }

                @Override
                public HttpClient.Version version() {
                    return null;
                }
            }
        );
        final ProbeResult r1 = probe.apply(p1, ctx);

        assertThat(r1.status()).isEqualTo(ResultStatus.SUCCESS);
        assertThat(r1.message()).isEqualTo("The plugin SCM link is valid");
    }

    @Test
    public void shouldRecognizeInvalidGitHubUrl() throws Exception {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final SCMLinkValidationProbe probe = new SCMLinkValidationProbe(httpClient, githubConfiguration);

        when(p1.getScm()).thenReturn("https://github.com/jenkinsci/this-is-not-going-to-work");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(
            new HttpResponse<>() {
                @Override
                public int statusCode() {
                    return 404;
                }

                @Override
                public HttpRequest request() {
                    return null;
                }

                @Override
                public Optional<HttpResponse<String>> previousResponse() {
                    return Optional.empty();
                }

                @Override
                public HttpHeaders headers() {
                    return null;
                }

                @Override
                public String body() {
                    return null;
                }

                @Override
                public Optional<SSLSession> sslSession() {
                    return Optional.empty();
                }

                @Override
                public URI uri() {
                    return null;
                }

                @Override
                public HttpClient.Version version() {
                    return null;
                }
            }
        );
        final ProbeResult r1 = probe.apply(p1, ctx);

        assertThat(r1.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r1.message()).isEqualTo("The plugin SCM link is invalid");
    }
}
