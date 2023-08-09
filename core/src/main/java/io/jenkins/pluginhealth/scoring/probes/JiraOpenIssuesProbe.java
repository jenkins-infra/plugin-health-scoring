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

import java.net.MalformedURLException;
import java.net.URL;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Order(AbstractOpenIssuesProbe.ORDER)
class JiraOpenIssuesProbe extends AbstractOpenIssuesProbe {
    public static final String KEY = "jira-open-issues";
    private static final String JIRA_HOST = "https://issues.jenkins.io/rest/api/latest/search?";
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraOpenIssuesProbe.class);
    RestTemplate restTemplate = new RestTemplate();

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        return getNumberOfOpenIssues(plugin, context);
    }

    /**
     * Get total number of open JIRA issues in a plugin
     */
    @Override
    ProbeResult getNumberOfOpenIssues(Plugin plugin, ProbeContext context) {
        String viewJiraIssuesUrl = context.getIssueTrackerNameAndUrl().get("jira");

        try {
            if (viewJiraIssuesUrl == null || viewJiraIssuesUrl.isEmpty()) {
                return ProbeResult.failure(key(), String.format("JIRA issues not found in Update Center for %s plugin.",  plugin.getName()));
            }
            URL url = new URL(viewJiraIssuesUrl);
            String api = JIRA_HOST.concat(url.getQuery()).concat(" AND status=open");

            ResponseEntity<String> response = restTemplate.getForEntity(api, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = response.getBody();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            if (jsonNode.get("errorMessages") != null) {
                return ProbeResult.error(key(), String.format("Error returned from JIRA API for plugin %s. %s",  plugin.getName(), jsonNode.get("errorMessages")));
            }
            int openJIRAIssues = jsonNode.get("total").asInt();
            return ProbeResult.success(key(), String.format("%d open issues found in JIRA.", openJIRAIssues));
        } catch (JsonMappingException e) {
            LOGGER.error("Cannot map JSON returned by JIRA API for plugin {}.", plugin.getName(), e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot process JSON returned by JIRA API for plugin {}.",  plugin.getName(), e);
        } catch (MalformedURLException e) {
            LOGGER.error("Cannot process malformed URL for plugin {}.",  plugin.getName(), e);
        }
        return ProbeResult.error(key(), String.format("Cannot fetch information from JIRA API for plugin %s.",  plugin.getName()));
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Returns total number of open issues in JIRA.";
    }
}
