package io.jenkins.pluginhealth.scoring.probes;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(RenovateProbe.ORDER)
public class RenovateProbe extends AbstractDetectBotConfigurationProbe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "renovate";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Check if Renovate is configured in the plugin";
    }

    @Override
    public String getBotToDetect() {
        return "renovate";
    }

    @Override
    public String getSuccessMessage() {
        return "Renovate is configured";
    }

    @Override
    public String getFailureMessage() {
        return "No configuration file for Renovate";
    }
}
