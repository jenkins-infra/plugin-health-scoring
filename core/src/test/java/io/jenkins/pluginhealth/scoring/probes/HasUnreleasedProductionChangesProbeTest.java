package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
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

public class HasUnreleasedProductionChangesProbeTest extends AbstractProbeTest<HasUnreleasedProductionChangesProbe> {
    @Override
    HasUnreleasedProductionChangesProbe getSpy() {
        return spy(HasUnreleasedProductionChangesProbe.class);
    }

    @Test
    void shouldBeExecutedAfterLastCommitDateProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final HasUnreleasedProductionChangesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(HasUnreleasedProductionChangesProbe.KEY, ""));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldCheckIfTheUnreleasedCommitsExist() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";

        ZonedDateTime releaseTimestamp = ZonedDateTime.now().minusHours(38);
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getScm()).thenReturn(scmLink);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));
            Files.createFile(repository.resolve("README.md"));
            final Path srcMainResources = Files.createDirectories(repository.resolve("src").resolve("main")
                .resolve("resources"));
             Files.createFile(srcMainResources.resolve("test.txt"));

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(7).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();

            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Imports readme").setSign(false).setCommitter(committer).call();

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();

        }
        final HasUnreleasedProductionChangesProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(result.failure(HasUnreleasedProductionChangesProbe.KEY, "Unreleased commits exists in the plugin"));

    }

    @Test
    void commitOnPomFileBeforeLatestReleaseDateShouldReturnSuccess() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";
        final String pluginName = "test-plugin";

        ZonedDateTime releaseTimestamp = ZonedDateTime.now().plusHours(38);
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusDays(1).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();
        }

        final HasUnreleasedProductionChangesProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(result.success(HasUnreleasedProductionChangesProbe.KEY, ""));
    }

    @Test
    void commitOnReadmeFileAfterReleaseDateShouldReturnSuccess() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";

        ZonedDateTime releaseTimestamp = ZonedDateTime.now().minusHours(38);
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getScm()).thenReturn(scmLink);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("README.md"));

            // creating commit

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(7).toInstant()));

            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Updated README.md file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            final ProbeResult result = probe.apply(plugin, ctx);

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status")
                .isEqualTo(result.success(HasUnreleasedProductionChangesProbe.KEY, ""));
        }
    }

    @Test
    void checkThatCommitOnSrcPathBeforeReleaseDateReturnsSuccess() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";

        ZonedDateTime releaseTimestamp = ZonedDateTime.now().plusHours(38);
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);
        when(plugin.getScm()).thenReturn(scmLink);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path srcMainResources = Files.createDirectories(repository.resolve("src").resolve("main")
                .resolve("resources"));
            Files.createFile(srcMainResources.resolve("test.txt"));

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusDays(7).toInstant()));

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            final ProbeResult result = probe.apply(plugin, ctx);

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(result.success(HasUnreleasedProductionChangesProbe.KEY, "All the commits have been released successfully for the plugin."));
        }

    }

    @Test
    void ifCommitExistsOnPomFileAfterLatestReleaseItShouldFail() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";
        final String pluginName = "test-plugin";

        ZonedDateTime releaseTimestamp = ZonedDateTime.now().minusHours(38);
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("pom.xml"));

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(7).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            final ProbeResult result = probe.apply(plugin, ctx);

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(result.failure(HasUnreleasedProductionChangesProbe.KEY, "Unreleased commits exists in the plugin"));
        }
    }

    @Test
    void commitOnSrcPathAfterReleaseDateShouldFail() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";

        ZonedDateTime releaseTimestamp = ZonedDateTime.now().minusHours(38);
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);
        when(plugin.getScm()).thenReturn(scmLink);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path srcMainResources = Files.createDirectories(repository.resolve("src").resolve("main")
                .resolve("resources"));
            Files.createFile(srcMainResources.resolve("test.txt"));

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(7).toInstant()));

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            final ProbeResult result = probe.apply(plugin, ctx);

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(result.failure(HasUnreleasedProductionChangesProbe.KEY, "Unreleased commits exists in the plugin"));
        }

    }

    @Test
    void commitOnReadmeFileBeforeReleaseDateShouldReturnSuccess() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String scmLink = "https://test-server/jenkinsci/test-repo/test-folder";

        ZonedDateTime releaseTimestamp = ZonedDateTime.now().plusHours(38);
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);
        when(plugin.getScm()).thenReturn(scmLink);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            Files.createFile(repository.resolve("README.md"));

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusDays(7).toInstant()));

            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Updated README.md file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();
            final ProbeResult result = probe.apply(plugin, ctx);

            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status")
                .isEqualTo(result.success(HasUnreleasedProductionChangesProbe.KEY, ""));
        }
    }
}
