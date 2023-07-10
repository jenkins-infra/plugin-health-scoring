package io.jenkins.pluginhealth.scoring.probes;

import static org.mockito.Mockito.spy;

public class RenovateProbeTest extends AbstractProbeTest<RenovateProbe> {
    @Override
    RenovateProbe getSpy() {
        return spy(RenovateProbe.class);
    }
}
