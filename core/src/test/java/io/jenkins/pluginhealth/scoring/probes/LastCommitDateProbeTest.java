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

package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;

class LastCommitDateProbeTest extends AbstractProbeTest<LastCommitDateProbe> {
    @Override
    LastCommitDateProbe getSpy() {
        return spy(LastCommitDateProbe.class);
    }

    @Test
    void shouldNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void shouldNotBeRelatedToSourceCode() {
        assertThat(getSpy().isSourceCodeRelated()).isFalse();
    }

    @Test
    void shouldBeExecutedAfterSCMLinkValidation() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = getSpy();

        final String pluginName = "foo";
        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getScmRepository()).thenReturn(Optional.empty());

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(LastCommitDateProbe.KEY, "There is no local repository for plugin " + pluginName + ".", probe.getVersion()));
    }

    @Test
    void shouldReturnSuccessStatusOnValidSCM() throws IOException, GitAPIException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = getSpy();

        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/parameterized-trigger-plugin.git");
        final Path repo = Files.createTempDirectory(UUID.randomUUID().toString());
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        final ZoneId commitZoneId = ZoneId.of("Europe/Paris");
        final ZonedDateTime commitDate = ZonedDateTime.now(commitZoneId)
            .minusHours(1).minusMinutes(2)
            .truncatedTo(ChronoUnit.SECONDS);

        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            git.commit()
                .setAllowEmpty(true)
                .setSign(false)
                .setMessage("This commit")
                .setCommitter(new PersonIdent("Foo", "foo@bar.xyz", commitDate.toInstant(), commitZoneId))
                .call();
        }

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.success(LastCommitDateProbe.KEY, "", probe.getVersion()));

        final ZonedDateTime parsedDateTime = ZonedDateTime.parse(result.message(), DateTimeFormatter.ISO_DATE_TIME);
        assertThat(parsedDateTime).isEqualTo(commitDate);
    }
}
