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

import java.util.Map;
import java.util.Properties;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

class MavenPropertiesProbeTest extends AbstractProbeTest<MavenPropertiesProbe> {
    @Override
    MavenPropertiesProbe getSpy() {
        return spy(MavenPropertiesProbe.class);
    }

    @Test
    void shouldAcceptNoProperties() throws Exception {
        final Model pom = mock(Model.class);
        final MavenPropertiesProbe probe = getSpy();

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(MavenPropertiesProbe.KEY, Map.of(), probe.getVersion()));
    }

    @Test
    void shouldAcceptProperties() throws Exception {
        final Model pom = mock(Model.class);
        final MavenPropertiesProbe probe = getSpy();

        final Properties prop = new Properties(1);
        prop.put("jenkins.baseline", "1.564");
        when(pom.getProperties()).thenReturn(prop);

        assertThat(probe.getMavenDetails(pom))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(
                        MavenPropertiesProbe.KEY, Map.of("jenkins.baseline", "1.564"), probe.getVersion()));
    }
}
