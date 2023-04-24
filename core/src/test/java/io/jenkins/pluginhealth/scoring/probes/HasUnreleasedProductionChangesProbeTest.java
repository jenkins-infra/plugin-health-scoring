package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HasUnreleasedProductionChangesProbeTest extends AbstractProbeTest<HasUnreleasedProductionChangesProbe> {
    @Override
    HasUnreleasedProductionChangesProbe getSpy() {
        return spy(HasUnreleasedProductionChangesProbe.class);
    }

    @Test
    void shouldBeExecutedAfterLasCommitDateProbe() {
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
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final HasUnreleasedProductionChangesProbe probe = getSpy();

        ZonedDateTime releaseTimestamp = ZonedDateTime.now();
        when(plugin.getReleaseTimestamp()).thenReturn(releaseTimestamp);

//        ref: https://www.vogella.com/tutorials/JGit/article.html
//        creating a fake Git repository

        Path parentDir = Paths.get("/path/to/local");
        Path tempDir = Files.createTempDirectory(parentDir, "mytempdir");

        Path srcTempDir = Files.createTempDirectory(tempDir, "filedir");
//        create a commit
//        https://stackoverflow.com/a/51151158
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // create files to add to the repo

        File file1 = new File(tempDir.toString(), "pom.xml");
        File file2 = new File(tempDir.toString(), "README.md");
        File file3 = new File(tempDir.toString(), String.valueOf(srcTempDir));
        file1.createNewFile();
        file2.createNewFile();
        file3.createNewFile();

        AddCommand add = git.add();
        // Stage all files in the repo
        add.addFilepattern("pom.xml").addFilepattern("README.md").addFilepattern("src/test/resources/file.txt").call();
        Repository repository = new FileRepositoryBuilder().setGitDir(tempDir.toFile()).build();

        // create committer
        PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
        PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(1).toInstant()));

        // commit the files
        CommitCommand commit = git.commit();
        commit.setOnly( "pom.xml" ).setOnly( "README.md" ).setOnly("src/test/resources/file.txt").
            setCommitter(committer).
            setMessage("initial commit").call();
    }


}
