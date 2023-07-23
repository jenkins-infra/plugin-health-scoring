package io.jenkins.pluginhealth.scoring.probes;

import java.util.*;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = OpenIssuesProbe.ORDER)
public class OpenIssuesProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIssuesProbe.class);

    public static final String KEY = "open-issue";
    public static final int ORDER = UpdateCenterPluginPublicationProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        List<String> issueTracker = getIssueTracker(plugin, context);
        for (String type: issueTracker) {
            if(type.equals("jira")) {
                return ProbeResult.success(key(), String.format("%d open issues found", getJiraIssues()));
            }
            else if (type.equals("github")) {
                return ProbeResult.success(key(), String.format("%d open issues found", getGitHubIssues()));
            }
        }
        return ProbeResult.failure(key(), String.format("Update center issue tracker could not be found"));
    }

    private static List<String> getIssueTracker(Plugin plugin, ProbeContext context) {
        return context.getUpdateCenter().plugins()
            .get(plugin.getIssueTracker())
            .issueTrackers().stream()
            .flatMap(map -> map.entrySet().stream())
            .filter(entry -> entry.getKey().equals("type"))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    private static int getJiraIssues() {
        return 0;
    }

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
