/*
 * MIT License
 *
 * Copyright (c) 2025 Jenkins Infra
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

import java.util.List;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

class MavenDependenciesManagementProbeTest extends AbstractProbeTest<MavenDependenciesManagementProbe> {

    @Override
    MavenDependenciesManagementProbe getSpy() {
        return spy(MavenDependenciesManagementProbe.class);
    }

    @Test
    void shouldBeAbleToAcceptNoDependencyManagement() throws Exception {
        Model pom = mock(Model.class);
        final MavenDependenciesManagementProbe probe = getSpy();

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(MavenDependenciesManagementProbe.KEY, List.of(), probe.getVersion()));
    }

    @Test
    void shouldBeAbleToAcceptEmptyDependencyManagement() throws Exception {
        Model pom = mock(Model.class);
        final MavenDependenciesManagementProbe probe = getSpy();

        when(pom.getDependencyManagement()).thenReturn(new DependencyManagement());

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(MavenDependenciesManagementProbe.KEY, List.of(), probe.getVersion()));
    }

    @Test
    void shouldBeAbleToAcceptDependencyManagement() throws Exception {
        Model pom = mock(Model.class);
        final MavenDependenciesManagementProbe probe = getSpy();

        final DependencyManagement dm = new DependencyManagement();
        final Dependency dependency = new Dependency();
        dependency.setGroupId("groupId");
        dependency.setArtifactId("artifactId");
        dependency.setVersion("version");
        dm.addDependency(dependency);
        when(pom.getDependencyManagement()).thenReturn(dm);

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(
                        MavenDependenciesManagementProbe.KEY,
                        List.of("groupId:artifactId:version"),
                        probe.getVersion()));
    }
}
