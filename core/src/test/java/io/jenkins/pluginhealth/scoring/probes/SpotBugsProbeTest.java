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
 import java.time.ZonedDateTime;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 
 import io.jenkins.pluginhealth.scoring.model.Plugin;
 import io.jenkins.pluginhealth.scoring.model.ProbeResult;
 import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;
 
 import hudson.util.VersionNumber;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.kohsuke.github.GHCheckRun;
 import org.kohsuke.github.GHRepository;
 import org.kohsuke.github.GitHub;
 import org.kohsuke.github.PagedIterable;
 import org.mockito.junit.jupiter.MockitoExtension;
 
 @ExtendWith(MockitoExtension.class)
 class SpotBugsProbeTest {
     @Test
     public void shouldHaveSpecificKey() {
         assertThat(spy(SpotBugsProbe.class).key()).isEqualTo(SpotBugsProbe.KEY);
     }
 
     @Test
     public void shouldHaveDescription() {
         assertThat(spy(SpotBugsProbe.class).getDescription()).isNotBlank();
     }
 
     @Test
     public void shouldNotRequireRelease() {
         assertThat(spy(SpotBugsProbe.class).requiresRelease()).isFalse();
     }
 
     @Test
     public void shouldBeRelatedToCode() {
         assertThat(spy(SpotBugsProbe.class).isSourceCodeRelated()).isTrue();
     }
 
     @Test
     public void shouldRequireJenkinsfile() {
         final Plugin plugin = mock(Plugin.class);
         final ProbeContext ctx = mock(ProbeContext.class);
 
         when(plugin.getDetails()).thenReturn(
             Map.of(),
             Map.of(
                 JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
             )
         );
 
         final SpotBugsProbe probe = new SpotBugsProbe();
 
         // ProbeResult missing
         assertThat(probe.apply(plugin, ctx))
             .usingRecursiveComparison()
             .comparingOnlyFields("id", "status", "message")
             .isEqualTo(ProbeResult.error(SpotBugsProbe.KEY, "Requires Jenkinsfile"));
 
         // ProbeResult failure
         assertThat(probe.apply(plugin, ctx))
             .usingRecursiveComparison()
             .comparingOnlyFields("id", "status", "message")
             .isEqualTo(ProbeResult.error(SpotBugsProbe.KEY, "Requires Jenkinsfile"));
     }
 
     @Test
     public void shouldFailWhenRepositoryIsNotInOrganization() {
         final String pluginName = "foo";
         final String scmLink = "foo-bar";
         final String defaultBranch = "main";
 
         final Plugin plugin = mock(Plugin.class);
         final ProbeContext ctx = mock(ProbeContext.class);
 
 
         when(plugin.getName()).thenReturn(pluginName);
         when(plugin.getScm()).thenReturn(scmLink);
         when(plugin.getDetails()).thenReturn(Map.of(
             JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
         ));
 
         when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
             Map.of(
                 pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                     pluginName, new VersionNumber("1.0"), scmLink, ZonedDateTime.now(), List.of(), 0,
                     "42", defaultBranch
                 )
             ),
             Map.of(),
             List.of()
         ));
         when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.empty());
 
         final SpotBugsProbe probe = new SpotBugsProbe();
         final ProbeResult result = probe.apply(plugin, ctx);
 
         assertThat(result)
             .usingRecursiveComparison()
             .comparingOnlyFields("id", "status", "message")
             .isEqualTo(ProbeResult.failure(SpotBugsProbe.KEY, "Cannot determine plugin repository"));
     }
 
     @SuppressWarnings("unchecked")
     @Test
     public void shouldBeAbleToRetrieveDetailsFromGitHubChecks() throws IOException {
         final String pluginName = "mailer";
         final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
         final String scmLink = "https://github.com/" + pluginRepo;
         final String defaultBranch = "main";
 
         final Plugin plugin = mock(Plugin.class);
         final ProbeContext ctx = mock(ProbeContext.class);
 
         final GitHub gh = mock(GitHub.class);
         final GHRepository ghRepository = mock(GHRepository.class);
 
         when(plugin.getName()).thenReturn(pluginName);
         when(plugin.getDetails()).thenReturn(Map.of(
             JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
         ));
         when(plugin.getScm()).thenReturn(scmLink);
         when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
             Map.of(
                 pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                     pluginName, new VersionNumber("1.0"), scmLink, ZonedDateTime.now(), List.of(), 0,
                     "42", defaultBranch
                 )
             ),
             Map.of(),
             List.of()
         ));
         when(ctx.getGitHub()).thenReturn(gh);
         when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of(pluginRepo));
 
         when(gh.getRepository(pluginRepo)).thenReturn(ghRepository);
         final PagedIterable<GHCheckRun> checkRuns = (PagedIterable<GHCheckRun>) mock(PagedIterable.class);
         final GHCheckRun checkRun = mock(GHCheckRun.class);
         final GHCheckRun.Output output = mock(GHCheckRun.Output.class);
         when(checkRuns.toList()).thenReturn(
             List.of(checkRun)
         );
         when(ghRepository.getCheckRuns(defaultBranch, Map.of("check_name", "SpotBugs")))
             .thenReturn(checkRuns);
 
         final SpotBugsProbe probe = new SpotBugsProbe();
         final ProbeResult result = probe.apply(plugin, ctx);
 
         assertThat(result)
             .usingRecursiveComparison()
             .comparingOnlyFields("id", "status", "message")
             .isEqualTo(ProbeResult.success(SpotBugsProbe.KEY, "SpotBugs found in build configuration"));
     }
 
     @SuppressWarnings("unchecked")
     @Test
     public void shouldFailIfThereIsNoSpotBugs() throws IOException {
         final String pluginName = "mailer";
         final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
         final String scmLink = "https://github.com/" + pluginRepo;
         final String defaultBranch = "main";
 
         final Plugin plugin = mock(Plugin.class);
         final ProbeContext ctx = mock(ProbeContext.class);
 
         final GitHub gh = mock(GitHub.class);
         final GHRepository ghRepository = mock(GHRepository.class);
 
         when(plugin.getName()).thenReturn(pluginName);
         when(plugin.getDetails()).thenReturn(Map.of(
             JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
         ));
         when(plugin.getScm()).thenReturn(scmLink);
         when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
             Map.of(
                 pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                     pluginName, new VersionNumber("1.0"), scmLink, ZonedDateTime.now(), List.of(), 0,
                     "42", defaultBranch
                 )
             ),
             Map.of(),
             List.of()
         ));
         when(ctx.getGitHub()).thenReturn(gh);
         when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of(pluginRepo));
 
         when(gh.getRepository(pluginRepo)).thenReturn(ghRepository);
         final PagedIterable<GHCheckRun> checkRuns = (PagedIterable<GHCheckRun>) mock(PagedIterable.class);
         when(ghRepository.getCheckRuns(defaultBranch, Map.of("check_name", "SpotBugs")))
             .thenReturn(checkRuns);
 
         final SpotBugsProbe probe = new SpotBugsProbe();
         final ProbeResult result = probe.apply(plugin, ctx);
 
         assertThat(result)
             .usingRecursiveComparison()
             .comparingOnlyFields("id", "status", "message")
             .isEqualTo(ProbeResult.failure(SpotBugsProbe.KEY, "SpotBugs not found in build configuration"));
     }
 }
 