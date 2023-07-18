package io.jenkins.pluginhealth.scoring.probes;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
* Looks for Renovate bot configuration in a plugin.
*
* @see io.jenkins.pluginhealth.scoring.probes.AbstractDependencyBotConfigurationProbe
*/
@Component
@Order(AbstractDependencyBotConfigurationProbe.ORDER)
public class RenovateProbe extends AbstractDependencyBotConfigurationProbe {
    public static final int ORDER = AbstractDependencyBotConfigurationProbe.ORDER;
    public static final String KEY = "renovate";

    RenovateProbe() {
        super(KEY);
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks if Renovate is configured in a plugin.";
    }
}
