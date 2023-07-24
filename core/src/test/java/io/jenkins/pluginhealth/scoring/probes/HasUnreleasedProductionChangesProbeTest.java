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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;

class HasUnreleasedProductionChangesProbeTest extends AbstractProbeTest<HasUnreleasedProductionChangesProbe> {
    @Override
    HasUnreleasedProductionChangesProbe getSpy() {
        return spy(HasUnreleasedProductionChangesProbe.class);
    }

    @Test
    void shouldBeExecutedAfterLastCommitDateProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final HasUnreleasedProductionChangesProbe probe = getSpy();

        when(plugin.getName()).thenReturn("foo-bar");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(HasUnreleasedProductionChangesProbe.KEY, ""));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldFailIfThereIsNotReleasedCommits() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));
            final Path srcMainResources = Files.createDirectories(
                repository.resolve("src").resolve("main").resolve("resources")
            );
            Files.createFile(srcMainResources.resolve("index.jelly"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusHours(1).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();

        }
        final HasUnreleasedProductionChangesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(HasUnreleasedProductionChangesProbe.KEY, "Unreleased production modifications might exist in the plugin source code at pom.xml, src/main/resources/index.jelly"));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }

    @Test
    void shouldFailIfThereIsNotReleasedCommitsInModule() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(ctx.getScmFolderPath()).thenReturn("test-folder");

        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));
            final Path module = Files.createDirectory(repository.resolve("test-folder"));
            Files.createFile(module.resolve("pom.xml"));
            final Path srcMainResources = Files.createDirectories(
                module.resolve("src").resolve("main").resolve("resources")
            );
            Files.createFile(srcMainResources.resolve("index.jelly"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusHours(1).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();

            git.add().addFilepattern("test-folder").call();
            git.commit().setMessage("Imports module files").setSign(false).setCommitter(committer).call();

        }
        final HasUnreleasedProductionChangesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(HasUnreleasedProductionChangesProbe.KEY, "Unreleased production modifications might exist in the plugin source code at pom.xml, test-folder/pom.xml, test-folder/src/main/resources/index.jelly"));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }

    @Test
    void shouldSucceedWhenCommitOnPomFileBeforeLatestReleaseDate() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";
        final String pluginName = "test-plugin";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusHours(1).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();
        }

        final HasUnreleasedProductionChangesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(HasUnreleasedProductionChangesProbe.KEY, "All production modifications were released."));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }

    @Test
    void shouldSucceedWhenCommitOnReadmeFileAfterReleaseDate() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("README.md"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusHours(1).toInstant()));

            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Updated README.md file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(HasUnreleasedProductionChangesProbe.KEY, "All production modifications were released."));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldSucceedWhenCommitOnSrcMainPathBeforeReleaseDate() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getScm()).thenReturn(scmLink);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path srcMainResources = Files.createDirectories(repository.resolve("src").resolve("main")
                .resolve("resources"));
            Files.createFile(srcMainResources.resolve("test.txt"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusHours(1).toInstant()));

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(HasUnreleasedProductionChangesProbe.KEY, "All production modifications were released."));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldFailIfCommitExistsOnPomFileAfterLatestRelease() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";
        final String pluginName = "test-plugin";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(7).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(HasUnreleasedProductionChangesProbe.KEY, "Unreleased production modifications might exist in the plugin source code at pom.xml"));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldFailWhenCommitOnSrcPathAfterReleaseDate() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getScm()).thenReturn(scmLink);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path srcMainResources = Files.createDirectories(
                repository.resolve("src").resolve("main").resolve("resources")
            );
            Files.createFile(srcMainResources.resolve("index.jelly"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusHours(1).toInstant()));

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(HasUnreleasedProductionChangesProbe.KEY, "Unreleased production modifications might exist in the plugin source code at src/main/resources/index.jelly"));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldSucceedWhenCommitOnReadmeFileBeforeReleaseDate() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("README.md"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusHours(1).toInstant()));

            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Updated README.md file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(HasUnreleasedProductionChangesProbe.KEY, "All production modifications were released."));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldSucceedWhenCommitOnTestSourcesAfterReleaseDate() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getScm()).thenReturn(scmLink);
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {
            final Path srcTestJava = Files.createDirectories(repository.resolve("src").resolve("test").resolve("java"));
            Files.createFile(srcTestJava.resolve("TestA.java"));

            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusHours(1).toInstant()));

            git.add().addFilepattern("src/test").call();
            git.commit().setMessage("Import test").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            final ProbeResult result = probe.apply(plugin, ctx);

            assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(HasUnreleasedProductionChangesProbe.KEY, "All production modifications were released."));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldBeAbleToDifferentiateFilesWithCommitsBeforeAndAfterReleaseDate() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";
        final String pluginName = "test-plugin";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));
            PersonIdent committer = new PersonIdent(defaultCommitter, plugin.getReleaseTimestamp().minusHours(3).toInstant().getEpochSecond(), 0);
            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file before commit date").setSign(false).setCommitter(committer).call();

            final Path srcMainJava = Files.createDirectories(repository.resolve("src").resolve("main").resolve("java"));
            Files.createFile(srcMainJava.resolve("Hello.java"));
            PersonIdent committer2 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusHours(3).toInstant()));
            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Import main after commit  date").setSign(false).setCommitter(committer2).call();

            Files.createFile(repository.resolve("README.md"));
            PersonIdent committer3 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusHours(3).toInstant()));
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Updated README.md file after commit date").setSign(false).setCommitter(committer3).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(
                    HasUnreleasedProductionChangesProbe.KEY,
                    "Unreleased production modifications might exist in the plugin source code at src/main/java/Hello.java"
                ));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldBeAbleToOnlyDisplayProductionFilesInCommit() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";
        final String pluginName = "test-plugin";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));
            PersonIdent committer = new PersonIdent(defaultCommitter, plugin.getReleaseTimestamp().minusDays(3).toInstant().getEpochSecond(), 0);
            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file before commit date").setSign(false).setCommitter(committer).call();

            final Path srcMainJava = Files.createDirectories(repository.resolve("src").resolve("main").resolve("java"));
            Files.createFile(srcMainJava.resolve("Hello.java"));
            final Path srcTestJava = Files.createDirectories(repository.resolve("src").resolve("test").resolve("java"));
            Files.createFile(srcTestJava.resolve("HelloTest.java"));
            PersonIdent committer2 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(3).toInstant()));
            git.add().addFilepattern("src").call();
            git.commit().setMessage("Import class and test like for a bugfix").setSign(false).setCommitter(committer2).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(
                    HasUnreleasedProductionChangesProbe.KEY,
                    "Unreleased production modifications might exist in the plugin source code at src/main/java/Hello.java"
                ));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldBeAbleToDisplayProductionFilesInDifferentCommits() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";
        final String pluginName = "test-plugin";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));
            PersonIdent committer = new PersonIdent(defaultCommitter, plugin.getReleaseTimestamp().minusDays(3).toInstant().getEpochSecond(), 0);
            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file before commit date").setSign(false).setCommitter(committer).call();

            final Path srcMainJava = Files.createDirectories(repository.resolve("src").resolve("main").resolve("java"));
            Files.createFile(srcMainJava.resolve("Hello.java"));
            final Path srcTestJava = Files.createDirectories(repository.resolve("src").resolve("test").resolve("java"));
            Files.createFile(srcTestJava.resolve("HelloTest.java"));
            PersonIdent committer2 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(3).toInstant()));
            git.add().addFilepattern("src").call();
            git.commit().setMessage("Import class and test like for a bugfix").setSign(false).setCommitter(committer2).call();

            Files.createFile(srcMainJava.resolve("AnotherClass.java"));
            PersonIdent committer3 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(5).toInstant()));
            git.add().addFilepattern("src").call();
            git.commit().setMessage("New class for new feature").setSign(false).setCommitter(committer3).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(
                    HasUnreleasedProductionChangesProbe.KEY,
                    "Unreleased production modifications might exist in the plugin source code at src/main/java/AnotherClass.java, src/main/java/Hello.java"
                ));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }

    @Test
    void shouldBeAbleToDisplayProductionFilesInDifferentCommitsWithIntermediateCommits() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo";
        final String pluginName = "test-plugin";

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        final PersonIdent defaultCommitter = new PersonIdent(
            "Not real person", "this is not a real email"
        );

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("LICENSE"));
            PersonIdent committer0 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(4).toInstant()));
            git.add().addFilepattern("LICENSE").call();
            git.commit().setMessage("Import license file").setSign(false).setCommitter(committer0).call();

            Files.createFile(repository.resolve("pom.xml"));
            PersonIdent committer = new PersonIdent(defaultCommitter, plugin.getReleaseTimestamp().minusDays(3).toInstant().getEpochSecond(), 0);
            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file before commit date").setSign(false).setCommitter(committer).call();

            final Path srcMainJava = Files.createDirectories(repository.resolve("src").resolve("main").resolve("java"));
            Files.createFile(srcMainJava.resolve("Hello.java"));
            final Path srcTestJava = Files.createDirectories(repository.resolve("src").resolve("test").resolve("java"));
            Files.createFile(srcTestJava.resolve("HelloTest.java"));
            PersonIdent committer2 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(3).toInstant()));
            git.add().addFilepattern("src").call();
            git.commit().setMessage("Import class and test like for a bugfix").setSign(false).setCommitter(committer2).call();

            Files.createFile(repository.resolve("README.adoc"));
            PersonIdent committer3 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(4).toInstant()));
            git.add().addFilepattern("README.adoc").call();
            git.commit().setMessage("Import readme file").setSign(false).setCommitter(committer3).call();

            Files.createFile(srcMainJava.resolve("AnotherClass.java"));
            PersonIdent committer4 = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(5).toInstant()));
            git.add().addFilepattern("src").call();
            git.commit().setMessage("New class for new feature").setSign(false).setCommitter(committer4).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(
                    HasUnreleasedProductionChangesProbe.KEY,
                    "Unreleased production modifications might exist in the plugin source code at src/main/java/AnotherClass.java, src/main/java/Hello.java"
                ));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        }
    }
}
