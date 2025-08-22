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

import java.util.Collections;
import java.util.List;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

class MavenDependenciesProbeTest extends AbstractProbeTest<MavenDependenciesProbe> {
    @Override
    MavenDependenciesProbe getSpy() {
        return spy(MavenDependenciesProbe.class);
    }

    @Test
    void shouldAcceptNoDependencies() throws Exception {
        final Model pom = mock(Model.class);
        final MavenDependenciesProbe probe = getSpy();

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(MavenDependenciesProbe.KEY, List.of(), probe.getVersion()));
    }

    @Test
    void shouldAcceptEmptyDependencies() throws Exception {
        final Model pom = mock(Model.class);
        final MavenDependenciesProbe probe = getSpy();

        when(pom.getDependencies()).thenReturn(Collections.emptyList());

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(MavenDependenciesProbe.KEY, List.of(), probe.getVersion()));
    }

    @Test
    void shouldAcceptDependencies() throws Exception {
        final Model pom = mock(Model.class);
        final MavenDependenciesProbe probe = getSpy();

        final Dependency dep1 = new Dependency();
        dep1.setGroupId("groupId");
        dep1.setArtifactId("artifactId");
        dep1.setVersion("version");
        dep1.setOptional(false);
        final Dependency dep2 = new Dependency();
        dep2.setGroupId("other-groupId");
        dep2.setArtifactId("other-artifactId");
        dep2.setVersion("other-version");
        dep2.setOptional(true);

        when(pom.getDependencies()).thenReturn(List.of(dep1, dep2));

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(
                        MavenDependenciesProbe.KEY,
                        List.of(
                                "groupId:artifactId:version:false",
                                "other-groupId:other-artifactId:other-version:true"),
                        probe.getVersion()));
    }
}
