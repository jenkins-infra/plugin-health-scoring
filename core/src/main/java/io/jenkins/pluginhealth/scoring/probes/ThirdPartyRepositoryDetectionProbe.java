package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = ThirdPartyRepositoryDetectionProbe.ORDER)
public class ThirdPartyRepositoryDetectionProbe extends Probe{

    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyRepositoryDetectionProbe.class);
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "third-party-repository-detection-probe";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        return null;
    }

    @Override
    public String key() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
