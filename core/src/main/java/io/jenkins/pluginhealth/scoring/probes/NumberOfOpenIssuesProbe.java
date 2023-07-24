package io.jenkins.pluginhealth.scoring.probes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe counts the total number of open issues in GitHub and JIRA
 */
@Component
@Order(value = NumberOfOpenIssuesProbe.ORDER)
public class NumberOfOpenIssuesProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(NumberOfOpenIssuesProbe.class);
    public static final String KEY = "open-issue";
    public static final int ORDER = UpdateCenterPluginPublicationProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        List<String> issueTracker = getIssueTracker(context);
        for (String type : issueTracker) {
            if (type.equals("jira")) {
                return ProbeResult.success(key(), String.format("%d open issues found", getJiraIssues()));
            }
            else if (type.equals("github")) {
                return ProbeResult.success(key(), String.format("%d open issues found", getGitHubIssues()));
            }
        }
        return ProbeResult.failure(key(), "Update center issue tracker could not be found");
    }

    /**
     * Get issueTracker data from UpdateCenter and filter the type
     *
     * @param {@link io.jenkins.pluginhealth.scoring.probes#ProbeContext} the context data for the probe
     * @return a list which contains a map of issue tracker type
     */
    private static List<String> getIssueTracker(ProbeContext context) {
        return context.getUpdateCenter()
            .issueTrackers().stream()
            .flatMap(map -> map.entrySet().stream())
            .filter(entry -> entry.getKey().equals("type"))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Get total number of JIRA issues in a plugin
     * */
    private static int getJiraIssues() {
        return 0;
    }

    /**
     * Get total number of GitHub issues in a plugin
     * */
    private static int getGitHubIssues() {
        return 0;
    }


    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Returns the number of issues open in a plugin";
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{UpdateCenterPluginPublicationProbe.KEY};
    }

}
