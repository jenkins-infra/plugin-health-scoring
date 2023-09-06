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

import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(AbstractOpenIssuesProbe.ORDER)
class JiraOpenIssuesProbe extends AbstractOpenIssuesProbe {
    public static final String KEY = "jira-open-issues";
    private static final String JIRA_HOST = "https://issues.jenkins.io/rest/api/latest/search?";
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraOpenIssuesProbe.class);
    final ObjectMapper objectMapper = new ObjectMapper();
    HttpClient httpClient;
    HttpRequest httpRequest;

    /**
     * Get total number of open JIRA issues in a plugin.
     *
     * @param context {@link ProbeContext}
     * @return Optional of type Integer.
     */
    @Override
    Optional<Integer> getCountOfOpenIssues(ProbeContext context) {
        if (context.getIssueTrackerUrlsByNames() == null) {
            LOGGER.info("IssueTracker has no JIRA open issues for the plugin.");
            return Optional.empty();
        }

        /* Stores the JIRA URL to view all existing issues in the plugin. Ex: https://github.com/jenkinsci/cloudevents-plugin/issues */
        String viewJiraIssuesUrl = context.getIssueTrackerUrlsByNames().get("jira");

        if (viewJiraIssuesUrl == null || viewJiraIssuesUrl.isEmpty()) {
            LOGGER.info("The plugin does not use JIRA to track issues.");
            return Optional.empty();
        }
        try {
            /* The `url` will contain the JIRA url to view issues.
               For ex: https://issues.jenkins.io/rest/api/latest/search?jql=component=15979
            */
            URI uri = new URI(viewJiraIssuesUrl);

            /* Here, the query of the url "?jql=component=1833" is concatenated with " AND status=open".
               This gives the final API required to fetch JIRA issues.
               For ex: https://issues.jenkins.io/rest/api/latest/search?jql=component=15979%20and%20status=open
             */
            String api = JIRA_HOST.concat(uri.getQuery())
                .concat("%20AND%20")
                .concat("status=open");

            httpRequest = HttpRequest.newBuilder()
                .uri(new URI(api))
                .timeout(Duration.of(5, SECONDS)) // based on manual testing, timeout after 5 seconds works.
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String jsonResponse = response.body();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            if (jsonNode.get("errorMessages") != null) {
                LOGGER.error("Cannot request JIRA API for the plugin. {}", jsonNode.get("errorMessages"));
                return Optional.empty();
            }
            return Optional.of(jsonNode.get("total").asInt());
        } catch (JsonMappingException e) {
            LOGGER.error("Cannot map JSON returned by JIRA API for the plugin. {}", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot process JSON returned by JIRA API for the plugin. {}", e);
        } catch (MalformedURLException e) {
            LOGGER.error("Cannot process malformed URL for the plugin. {}", e);
        } catch (URISyntaxException e) {
            LOGGER.error("Incorrect URI syntax in the plugin. {}", e);
        } catch (IOException e) {
            LOGGER.error("Cannot read HttpResponse for the plugin. {}", e);
        } catch (InterruptedException e) {
            LOGGER.error("Interruption occurred when waiting for JIRA API for the plugin. {}", e);
        }
        return Optional.empty();
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
