package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOpenIssuesProbe extends Probe {
    public static final int ORDER = IssueTrackerDetectionProbe.ORDER + 100;
    private static final String JIRA_HOST = "https://issues.jenkins.io/rest/api/latest/search?";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpenIssuesProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        return null;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[] { IssueTrackerDetectionProbe.KEY};
    }

    public abstract String getTrackerType();

}
