package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.kohsuke.github.GHRepository;
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
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        List<String> issueTracker = getIssueTrackerType(context);
        return issueTracker.stream()
            .filter(type -> type.equals("jira"))
            .findFirst()
            .map(type -> getJiraIssues())
            .orElseGet(() -> issueTracker.stream()
                .filter(type -> type.equals("github"))
                .findFirst()
                .map(type -> getGitHubIssues(context, plugin.getScm()))
                .orElse(ProbeResult.failure(key(), "Update center issue tracker could not be found")));
    }

    /**
     * Get issueTracker data from UpdateCenter and filter the type
     *
     * @param {@link io.jenkins.pluginhealth.scoring.probes#ProbeContext} the context data for the probe
     * @return a list which contains a map of issue tracker type
     */
    private List<String> getIssueTrackerType(ProbeContext context) {
        return context.getUpdateCenter()
            .issueTrackers().stream()
            .flatMap(map -> map.entrySet().stream())
            .filter(entry -> entry.getKey().equals("type"))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Get total number of open JIRA issues in a plugin
     * */
    private ProbeResult getJiraIssues() {
        return null;
    }

    /**
     * Get total number of open GitHub issues in a plugin
     * */
    private ProbeResult getGitHubIssues(ProbeContext context, String scm) {
        try {
            final Optional<String> repositoryName = context.getRepositoryName(scm);
            if (repositoryName.isPresent()) {
                final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
                return ProbeResult.success(key(), String.format("%d open issues found", ghRepository.getOpenIssueCount()));
            }
        } catch (IOException ex) {
            return ProbeResult.error(key(), "Could not read GitHub open issues");
        }
        return ProbeResult.failure(key(), String.format("GitHub repository could not be found"));
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
        return new String[]{SCMLinkValidationProbe.KEY, UpdateCenterPluginPublicationProbe.KEY};
    }

}
