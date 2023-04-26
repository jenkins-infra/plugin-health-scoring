package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatPredicate;
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

        Path parentDir = Paths.get("/path/to/repository");
        //        create a commit
//        https://stackoverflow.com/a/51151158
        Git git = Git.init().setDirectory(parentDir.toFile()).call();

        // create files to add to the repo

        File file1 = new File(parentDir.toString(), "pom.xml");
        File file2 = new File(parentDir.toString(), "README.md");
        File directory = new File(parentDir+File.separator+"src"+ File.separator+"main"+File.separator+"resources"+File.separator+"test.txt");

        file1.createNewFile();
        file2.createNewFile();


        AddCommand add = git.add();
        // Stage all files in the repo
        add.addFilepattern(file1.getPath());
        add.addFilepattern(file2.getPath());
        add.addFilepattern(directory.getPath());
        add.call();

        Repository repository = new FileRepositoryBuilder().setGitDir(parentDir.toFile()).build();

        // create committer
        PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
        PersonIdent committer = new PersonIdent(defaultCommitter, Date.from(plugin.getReleaseTimestamp().plusDays(1).toInstant()));


        Path filePath = Paths.get("/path/to/repository/pom.xml");
        if (Files.exists(filePath)) {
            System.out.println("pom.xml file exists!");
        } else {
            System.out.println("pom.xml file does not exist.");
        }

        // commit file1
        CommitCommand commit1 = git.commit();
        commit1.setOnly(file1.getPath()).setCommitter(committer).
            setMessage("added pom.xml").call();

        // commit file2
        CommitCommand commit2 = git.commit();
        commit2.setOnly(file2.getPath()).setCommitter(committer).
            setMessage("added README file").call();

        // commit file3
        CommitCommand commit3 = git.commit();
        commit3.setOnly(directory.getPath()).setCommitter(committer).
            setMessage("added the directory").call();
    }


}
