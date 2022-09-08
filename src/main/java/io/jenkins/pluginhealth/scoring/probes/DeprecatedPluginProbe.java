package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

public class DeprecatedPluginProbe extends Probe implements UpdateCenterProbe {
    @Override
    protected ProbeResult doApply(Plugin plugin) {
        return null;
    }

    @Override
    public String key() {
        return "deprecated-plugin";
    }

    @Override
    public void runProbe(Plugin plugin) {

    }
}
