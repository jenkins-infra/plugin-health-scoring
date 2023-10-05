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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.kohsuke.github.GitHub;

public class ProbeContext {
    private final UpdateCenter updateCenter;
    private final Path scmRepository;
    private GitHub github;
    private ZonedDateTime lastCommitDate;
    private Map<String, String> pluginDocumentationLinks;
    private Map<String, String> issueTrackerUrlsByNames;
    private Optional<String> scmFolderPath;

    public ProbeContext(String pluginName, UpdateCenter updateCenter) throws IOException {
        this.updateCenter = updateCenter;
        this.scmRepository = Files.createTempDirectory(pluginName);
    }

    public UpdateCenter getUpdateCenter() {
        return updateCenter;
    }

    public Path getScmRepository() {
        return scmRepository;
    }

    public Optional<ZonedDateTime> getLastCommitDate() {
        return Optional.ofNullable(lastCommitDate);
    }

    public void setLastCommitDate(ZonedDateTime lastCommitDate) {
        this.lastCommitDate = lastCommitDate;
    }

    public GitHub getGitHub() {
        return github;
    }

    public void setGitHub(GitHub github) {
        this.github = github;
    }

    public Map<String, String> getPluginDocumentationLinks() {
        return pluginDocumentationLinks;
    }

    public void setPluginDocumentationLinks(Map<String, String> pluginDocumentationLinks) {
        this.pluginDocumentationLinks = pluginDocumentationLinks;
    }

    public Optional<String> getRepositoryName(String scm) {
        final Matcher match = SCMLinkValidationProbe.GH_PATTERN.matcher(scm);
        return match.find() ? Optional.of(match.group("repo")) : Optional.empty();
    }

    public void setIssueTrackerNameAndUrl(Map<String, String> issueTrackerNameAndUrl) {
        this.issueTrackerUrlsByNames = issueTrackerNameAndUrl;
    }

    /**
     * Gets the issue tracker names and its urls.
     *
     * @return a Map that consists of {@link io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers} "type" as key and "viewUrl" as its value.
     */
    public Map<String, String> getIssueTrackerUrlsByNames() {
        return issueTrackerUrlsByNames;
    }

    public Optional<String> getScmFolderPath() {
        return scmFolderPath;
    }

    public void setScmFolderPath(Optional<String> scmFolderPath) {
        this.scmFolderPath = scmFolderPath;
    }

    /* default */ void cleanUp() throws IOException {
        try (Stream<Path> paths = Files.walk(this.scmRepository)) {
            paths.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
