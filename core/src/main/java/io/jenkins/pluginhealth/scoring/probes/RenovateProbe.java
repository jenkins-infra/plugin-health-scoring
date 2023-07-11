package io.jenkins.pluginhealth.scoring.probes;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(RenovateProbe.ORDER)
public class RenovateProbe extends AbstractDetectBotConfigurationProbe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "renovate";

    RenovateProbe(String botName) {
        super(botName);
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Check if Renovate is configured in the plugin";
    }
}
