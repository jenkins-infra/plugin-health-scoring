package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

class DefaultBranchStatusProbeTest extends AbstractProbeTest<DefaultBranchStatusProbe> {

    private DefaultBranchStatusProbe defaultBranchStatusProbe;
    private Plugin mockPlugin;
    private ProbeContext mockContext;
    private GitHub mockGitHub;
    private GHRepository mockRepository;
    private GHCommit mockCommit;
    private GHCheckRun mockCheckRun;

    @Override
    DefaultBranchStatusProbe getSpy() {
        return spy(DefaultBranchStatusProbe.class);
    }

    @BeforeEach
    void setUp() {
        defaultBranchStatusProbe = new DefaultBranchStatusProbe();

        // Initialize mocks
        mockPlugin = mock(Plugin.class);
        mockContext = mock(ProbeContext.class);
        mockGitHub = mock(GitHub.class);
        mockRepository = mock(GHRepository.class);
        mockCommit = mock(GHCommit.class);
        mockCheckRun = mock(GHCheckRun.class);

        // Setup mock interactions
        when(mockContext.getRepositoryName()).thenReturn(Optional.empty());
        when(mockContext.getGitHub()).thenReturn(mockGitHub);
    }


    @Test
    void testBuildFailedInDefaultBranch() throws IOException {
        when(mockContext.getRepositoryName()).thenReturn(Optional.of("repositoryName"));
        when(mockRepository.getDefaultBranch()).thenReturn("main");
        when(mockGitHub.getRepository("repositoryName")).thenReturn(mockRepository);
        when(mockRepository.getCommit("main")).thenReturn(mockCommit);
        when(mockCheckRun.getConclusion()).thenReturn(GHCheckRun.Conclusion.FAILURE);

       final DefaultBranchStatusProbe probe = getSpy();
       final Plugin plugin = mock(Plugin.class);
       final ProbeContext ctx = mock(ProbeContext.class);

         assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(ContributingGuidelinesProbe.KEY, "No contributing guidelines found.", probe.getVersion()));
    }




}
