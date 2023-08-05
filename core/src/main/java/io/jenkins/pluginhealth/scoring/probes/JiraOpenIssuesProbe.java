package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(AbstractOpenIssuesProbe.ORDER)
class JiraOpenIssuesProbe extends AbstractOpenIssuesProbe {

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        return null;
    }

    @Override
    public String getTrackerType() {
        return "jira";
    }

    @Override
    public String key() {
        return "jira-open-issues";
    }

    @Override
    public String getDescription() {
        return "Returns total number of open issues in JIRA.";
    }
}
