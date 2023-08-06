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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractOpenIssuesProbe extends Probe {
    public static final int ORDER = IssueTrackerDetectionProbe.ORDER + 100;
    private static final String JIRA_HOST = "https://issues.jenkins.io/rest/api/latest/search?";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpenIssuesProbe.class);
    RestTemplate restTemplate = new RestTemplate();

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        Map<String, BiFunction<Plugin, ProbeContext, ProbeResult>> trackerMethods = createTrackerMethods();
        BiFunction<Plugin, ProbeContext, ProbeResult> getOpenIssues = trackerMethods.get(getTrackerType());

        if (getOpenIssues != null) {
            return getOpenIssues.apply(plugin, context);
        }
        return ProbeResult.error(key(), String.format("Cannot not find a valid issue tracker type for plugin {}", plugin.getName()));
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[] {SCMLinkValidationProbe.KEY, IssueTrackerDetectionProbe.KEY};
    }

    /**
     * @return a String the tracker "type" present in the "issueTrackers" in UpdateCenter. For ex: jira, github
     */
    public abstract String getTrackerType();

    private Optional<String> getTrackerUrl(Map<String, String> trackerType) {
        return trackerType.entrySet().stream()
            .filter(entry -> entry.getKey().equals(getTrackerType()))
            .findFirst()
            .map(entry -> entry.getValue());
    }

    /**
     * Get total number of open JIRA issues in a plugin
     */
    protected ProbeResult getJiraIssues(String viewJiraIssuesUrl, String pluginName) {
        try {
            if (viewJiraIssuesUrl.isEmpty()) {
                return ProbeResult.failure(key(), String.format("JIRA issues is not configured for %s plugin.", pluginName));
            }
            URL url = new URL(viewJiraIssuesUrl);
            String api = JIRA_HOST.concat(url.getQuery()).concat(" AND status=open");

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
        } catch (MalformedURLException e) {
            LOGGER.error("Cannot process malformed URL for plugin {}.", pluginName, e);
        }
        return ProbeResult.error(key(), String.format("Cannot fetch information from JIRA API for plugin %s.", pluginName));
    }

    /**
     * Get total number of open GitHub issues in a plugin
     */
    private ProbeResult getGitHubIssues(ProbeContext context, Plugin plugin) {
        try {
            final Optional<String> repositoryName = context.getRepositoryName(plugin.getScm());
            if (repositoryName.isPresent()) {
                final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
                int openGitHubIssues =  ghRepository.getOpenIssueCount();
                return ProbeResult.success(key(), String.format("%d open issues found in GitHub.", openGitHubIssues));
            }
        } catch (IOException ex) {
            return ProbeResult.error(key(), String.format("Cannot not read open issues on GitHub for plugin %s.", plugin.getName()));
        }
        return ProbeResult.failure(key(), String.format("Cannot find GitHub repository for plugin %s.", plugin.getName()));
    }

    private Map<String, BiFunction<Plugin, ProbeContext, ProbeResult>> createTrackerMethods() {
        Map<String, BiFunction<Plugin, ProbeContext, ProbeResult>> trackerMethods = new HashMap<>();
        trackerMethods.put("jira", (plugin, context) -> getJiraIssues(getTrackerUrl(context.getIssueTrackerType()).orElse(""), plugin.getName()));
        trackerMethods.put("github", (plugin, context) -> getGitHubIssues(context, plugin));
        return trackerMethods;
    }

}
