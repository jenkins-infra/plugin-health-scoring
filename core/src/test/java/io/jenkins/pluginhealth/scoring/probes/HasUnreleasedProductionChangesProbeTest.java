package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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

        ZonedDateTime releaseTimestamp = ZonedDateTime.now();
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path pom = Files.createFile(repository.resolve("pom.xml"));
            final Path readme = Files.createFile(repository.resolve("README.md"));
            final Path srcMainResources = Files.createDirectories(repository.resolve("src").resolve("main")
                .resolve("resources"));
            final Path test = Files.createFile(srcMainResources.resolve("test.txt"));

            // create committer
            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(1).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();

            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Imports readme").setSign(false).setCommitter(committer).call();

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();


            // returns a failure when the last commit date of any file in src/main folder is more recent than the plugin release timestamp
            // https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/WalkAllCommits.java
            // a RevWalk allows to walk over commits based on some filtering that is defined
            Collection<Ref> allRefs = git.getRepository().getAllRefs().values();

            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                for (Ref ref : allRefs) {
                    revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                }
                System.out.println("Walking all commits starting with " + allRefs.size() + " refs: " + allRefs);
                int count = 0;
                for (RevCommit commit : revWalk) {
                    System.out.println("Commit: " + commit);
                    System.out.println("Commit date: " + commit.getCommitTime());
                    System.out.println("plugin released date: " + plugin.getReleaseTimestamp());
                    Instant instant = Instant.ofEpochSecond(commit.getCommitTime());

                    // Convert to LocalDateTime in Asia/Calcutta timezone
                    LocalDateTime commitDate = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Calcutta"));
                    System.out.println("timestamp to date= " + commitDate);

                    LocalDateTime pluginReleaseDate = LocalDateTime.parse(plugin.getReleaseTimestamp().toString(), DateTimeFormatter.ISO_DATE_TIME);
                    assertThat(commitDate, greaterThan(pluginReleaseDate));
                    assertThat(plugin)
                        .usingRecursiveComparison()
                        .comparingOnlyFields("releaseTimestamp")
                        .isEqualTo(ProbeResult.error(HasUnreleasedProductionChangesProbe.KEY, ""));
                    count++;
                }
                System.out.println("Had " + count + " commits");
            }

        }
        final HasUnreleasedProductionChangesProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);


    }

    // check that a commit on pom.xml before the latest release is returning a SUCCESS.
    @Test
    void commitOnPomFileBeforeLatestReleaseDateShouldReturnSuccess() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        ZonedDateTime releaseTimestamp = ZonedDateTime.now();
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path pom = Files.createFile(repository.resolve("pom.xml"));

            // creating commit

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusDays(1).toInstant()));

            git.add().addFilepattern("pom.xml").call();
            git.commit().setMessage("Imports pom.xml file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            for (RevCommit commit : git.log().call()) {
                long timestamp = commit.getCommitTime();
                String dateString = plugin.getReleaseTimestamp().toString();

                Instant timestampInstant = Instant.ofEpochSecond(timestamp);
                DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, formatter);
                Instant dateInstant = zonedDateTime.toInstant();

                final ProbeResult result = probe.apply(plugin, ctx);
                assertThat(commit.getFullMessage().equals("Imports pom.xml file")).isTrue();
                assertThat(timestampInstant.isBefore(dateInstant)).isEqualTo(true);
            }
        }
    }

    @Test
    void commitOnReadmeFileAfterReleaseDateShouldReturnSuccess() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        ZonedDateTime releaseTimestamp = ZonedDateTime.now();
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path pom = Files.createFile(repository.resolve("README.md"));

            // creating commit

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(1).toInstant()));

            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Updated README.md file").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            for (RevCommit commit : git.log().call()) {
                long timestamp = commit.getCommitTime();
                String dateString = plugin.getReleaseTimestamp().toString();

                Instant timestampInstant = Instant.ofEpochSecond(timestamp);
                DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, formatter);
                Instant dateInstant = zonedDateTime.toInstant();

                final ProbeResult result = probe.apply(plugin, ctx);
                assertThat(commit.getFullMessage().equals("Updated README.md file")).isTrue();
                assertThat(dateInstant.isBefore(timestampInstant)).isEqualTo(true);
            }
        }
    }

    @Test
    void checkThatCommitOnSrcPathBeforeReleaseDateReturnsSuccess() throws IOException, GitAPIException {
        final Path repository = Files.createTempDirectory("test-foo-bar");

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final HasUnreleasedProductionChangesProbe hasUnreleasedProductionChangesProbe = mock(HasUnreleasedProductionChangesProbe.class);
        when(hasUnreleasedProductionChangesProbe.apply(plugin, ctx)).thenReturn(ProbeResult.success("unreleased-production-changes", ""));

        ZonedDateTime releaseTimestamp = ZonedDateTime.now();
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(repository);

        try (Git git = Git.init().setDirectory(repository.toFile()).call()) {

            final Path srcMainResources = Files.createDirectories(repository.resolve("src").resolve("main")
                .resolve("resources"));
            final Path test = Files.createFile(srcMainResources.resolve("test.txt"));

            // creating commit

            PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
            PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().minusDays(1).toInstant()));

            git.add().addFilepattern("src/main").call();
            git.commit().setMessage("Imports production files").setSign(false).setCommitter(committer).call();

            final HasUnreleasedProductionChangesProbe probe = getSpy();

            for (RevCommit commit : git.log().call()) {
                long timestamp = commit.getCommitTime();
                String dateString = plugin.getReleaseTimestamp().toString();

                Instant timestampInstant = Instant.ofEpochSecond(timestamp);
                DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, formatter);
                Instant dateInstant = zonedDateTime.toInstant();

                final ProbeResult result = probe.apply(plugin, ctx);
                assertThat(commit.getFullMessage().equals("Imports production files")).isTrue();
                assertThat(timestampInstant.isBefore(dateInstant)).isEqualTo(hasUnreleasedProductionChangesProbe.apply(plugin, ctx));
            }
        }
    }

}
