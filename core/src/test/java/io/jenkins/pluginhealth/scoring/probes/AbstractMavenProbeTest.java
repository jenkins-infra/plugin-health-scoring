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

import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

class AbstractMavenProbeTest extends AbstractProbeTest<AbstractMavenProbe> {
    private static class DefaultMavenProbe extends AbstractMavenProbe {

        public static final String KEY = "FOO";

        @Override
        protected ProbeResult getMavenDetails(Model pom) {
            return success("");
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        public String getDescription() {
            return "(empty)";
        }

        @Override
        public long getVersion() {
            return 1;
        }
    }

    @Override
    AbstractMavenProbe getSpy() {
        return spy(DefaultMavenProbe.class);
    }

    @Test
    void shouldNotAcceptNonExistentPOMFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final AbstractMavenProbe probe = getSpy();

        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/test-repo")));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.error(DefaultMavenProbe.KEY, "There is no pom.xml file for the plugin.", 1));
    }

    @Test
    void shouldNotAcceptInvalidPOMFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final AbstractMavenProbe probe = getSpy();

        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/test-repo/invalid")));

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status")
                .isEqualTo(ProbeResult.error(DefaultMavenProbe.KEY, "There is no pom.xml file for the plugin.", 1));
        assertThat(result.message())
                .isInstanceOf(String.class)
                .asString()
                .startsWith("Could not process project configuration");
    }
}
