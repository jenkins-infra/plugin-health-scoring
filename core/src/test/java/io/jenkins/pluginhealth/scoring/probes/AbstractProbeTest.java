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

import org.junit.jupiter.api.Test;

abstract class AbstractProbeTest<T extends Probe> {
    /**
     * Create a {@link org.mockito.Mockito#spy(Object)} spy} of the specific Probe implementation to be tested.
     *
     * @return a spy of the Probe been tested
     */
    abstract T getSpy();

    @Test
    void shouldUseValidKey() {
        assertThat(getSpy().key()).isNotBlank();
    }

    @Test
    void shouldHaveDescription() {
        assertThat(getSpy().getDescription()).isNotBlank();
    }

    @Test
    void shouldNotHaveNullRequirement() {
        assertThat(getSpy().getProbeResultRequirement()).isNotNull();
    }
/*
    @SuppressWarnings("unchecked")
    @Test
    void shouldValidateProbeRequirements() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Probe probe = getSpy();
        final int probeRequirementsLength = probe.getProbeResultRequirement().length;

        when(plugin.getDetails()).thenReturn(
            Map.of()
        );
        // TODO generate required mocking to validate the requirements

        final ProbeResult result = probe.apply(plugin, ctx);
        for (int i = 0; i < Math.pow(probeRequirementsLength, probeRequirementsLength) - probeRequirementsLength; i++) {
            assertThat(result.status()).isEqualTo(ResultStatus.ERROR);
            verify(probe, never()).doApply(plugin, ctx);
        }
    }*/
}
