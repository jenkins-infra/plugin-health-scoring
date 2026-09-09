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
package io.jenkins.pluginhealth.scoring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GitHub;
import org.springframework.boot.health.contributor.Status;

class GitHubHealthIndicatorTest {

    @Test
    void statusIsDownWhenGitHubIsNull() {
        var indicator = new GitHubHealthIndicator(null);
        var health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void statusIsDownWhenApiUrlCheckFails() throws IOException {
        var github = mock(GitHub.class);
        doThrow(new IOException("unreachable")).when(github).checkApiUrlValidity();

        var health = new GitHubHealthIndicator(github).health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void statusIsUpAndConnectionDetailsPresentWhenAuthenticated() throws IOException {
        var github = mock(GitHub.class);
        var rateLimit = mock(GHRateLimit.class);
        when(github.isAnonymous()).thenReturn(false);
        when(github.lastRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getLimit()).thenReturn(5000);
        when(rateLimit.getRemaining()).thenReturn(4999);
        when(rateLimit.getResetDate()).thenReturn(new Date(0));

        var health = new GitHubHealthIndicator(github).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("connection");
        @SuppressWarnings("unchecked")
        var connection = (Map<String, Object>) health.getDetails().get("connection");
        assertThat(connection).containsEntry("authenticated", true);
        assertThat(connection).containsKey("rateLimit");
    }

    @Test
    void statusIsOutOfServiceWhenAnonymous() throws IOException {
        var github = mock(GitHub.class);
        var rateLimit = mock(GHRateLimit.class);
        when(github.isAnonymous()).thenReturn(true);
        when(github.lastRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getLimit()).thenReturn(60);
        when(rateLimit.getRemaining()).thenReturn(59);
        when(rateLimit.getResetDate()).thenReturn(new Date(0));

        var health = new GitHubHealthIndicator(github).health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsKey("connection");
        @SuppressWarnings("unchecked")
        var connection = (Map<String, Object>) health.getDetails().get("connection");
        assertThat(connection).containsEntry("authenticated", false);
    }

    @Test
    void connectionDetailHasNoRateLimitKeyWhenLastRateLimitIsNull() throws IOException {
        var github = mock(GitHub.class);
        when(github.isAnonymous()).thenReturn(false);
        when(github.lastRateLimit()).thenReturn(null);

        var health = new GitHubHealthIndicator(github).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        @SuppressWarnings("unchecked")
        var connection = (Map<String, Object>) health.getDetails().get("connection");
        assertThat(connection).doesNotContainKey("rateLimit");
    }
}
