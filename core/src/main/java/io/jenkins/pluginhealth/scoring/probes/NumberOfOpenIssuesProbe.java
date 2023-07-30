/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * This probe counts the total number of open issues in GitHub and JIRA
 */
@Component
@Order(value = NumberOfOpenIssuesProbe.ORDER)
public class NumberOfOpenIssuesProbe extends Probe {
    public static final String KEY = "open-issue";
    public static final int ORDER = UpdateCenterPluginPublicationProbe.ORDER + 100;
    public static final String JIRA_HOST = "https://issues.jenkins.io/";
    public static final String JIRA_API_PATH = "rest/api/latest/search";
    private static final Logger LOGGER = LoggerFactory.getLogger(NumberOfOpenIssuesProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        List<String> issueTracker = getIssueTrackerType(context);

        if (issueTracker.size() > 1) {
            return ProbeResult.success(key(),
                getJiraIssues(context, plugin.getScm(), plugin.getName()).message().concat(" ") +
                getGitHubIssues(context, plugin.getScm(), plugin.getName()).message());
        }
        return issueTracker.stream()
            .filter(item -> item.equals("jira"))
            .findFirst()
            .map(entry -> getJiraIssues(context, plugin.getScm(), plugin.getName()))
            .orElseGet(() -> issueTracker.stream()
                .filter(item -> item.equals("github"))
                .findFirst()
                .map(entry -> getGitHubIssues(context, plugin.getScm(), plugin.getName()))
                .orElse(ProbeResult.failure(key(), String.format("Cannot find issue tracker for %s in update center", plugin.getName()))));
    }

    /**
     * Get issueTracker data from UpdateCenter and filter the type
     *
     * @param context the probe context data
     * @return a list which contains a map of issue tracker type
     */
    private List<String> getIssueTrackerType(ProbeContext context) {
        return context.getUpdateCenter()
            .issueTrackers().stream()
            .flatMap(map -> map.entrySet().stream())
            .filter(map -> map.getKey().equals("type"))
            .map(map -> map.getValue())
            .collect(Collectors.toList());
    }

    /**
     * Get total number of open JIRA issues in a plugin
     */
    private ProbeResult getJiraIssues(ProbeContext context, String scm, String pluginName) {
        try {
            Optional<String> repository = context.getRepositoryName(scm);
            String api = JIRA_HOST + JIRA_API_PATH + "?jql=component="
                + (repository.isPresent() ? context.getRepositoryName(scm).get().split("/")[1] : "")
                + " AND status=open";

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(api, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = response.getBody();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            int openJIRAIssues = jsonNode.get("total").asInt();
            return ProbeResult.success(key(), String.format("%d open issues found in JIRA.", openJIRAIssues));
        } catch (JsonMappingException e) {
            LOGGER.error("Cannot map JSON returned by JIRA API for plugin {}.", pluginName, e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot process JSON returned by JIRA API for plugin {}.", pluginName, e);
        }
        return ProbeResult.failure(key(), String.format("Cannot fetch information from JIRA API for plugin %s.", pluginName));
    }

    /**
     * Get total number of open GitHub issues in a plugin
     */
    private ProbeResult getGitHubIssues(ProbeContext context, String scm, String pluginName) {
        try {
            final Optional<String> repositoryName = context.getRepositoryName(scm);
            if (repositoryName.isPresent()) {
                final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
                int openGitHubIssues =  ghRepository.getOpenIssueCount();
                return ProbeResult.success(key(), String.format("%d open issues found in GitHub.", openGitHubIssues));
            }
        } catch (IOException ex) {
            return ProbeResult.error(key(), String.format("Cannot not read open issues on GitHub for plugin %s.", pluginName));
        }
        return ProbeResult.failure(key(), String.format("Cannot find GitHub repository for plugin %s.", pluginName));
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
