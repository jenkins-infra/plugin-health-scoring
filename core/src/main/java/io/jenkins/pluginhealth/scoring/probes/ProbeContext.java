/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jenkins Infra
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
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProbeContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProbeContext.class);

    private final Plugin plugin;
    private final UpdateCenter updateCenter;
    private Path scmRepository;
    private GitHub github;
    private ZonedDateTime lastCommitDate;
    private Map<String, String> pluginDocumentationLinks;
    private Path scmFolderPath;

    public ProbeContext(Plugin plugin, UpdateCenter updateCenter) {
        this.plugin = plugin;
        this.updateCenter = updateCenter;
    }

    public UpdateCenter getUpdateCenter() {
        return updateCenter;
    }

    public void cloneRepository() {
        if (scmRepository != null) {
            LOGGER.warn("The Git repository of this plugin was already cloned in {}.", scmRepository);
        }
        if (plugin.getScm() == null || plugin.getScm().isBlank()) {
            LOGGER.info("Cannot clone repository for {} because SCM link is `{}`", plugin.getName(), plugin.getScm());
            return;
        }
        final String pluginName = this.plugin.getName();
        try {
            final Path repo = Files.createTempDirectory(pluginName);
            try (Git git = Git.cloneRepository()
                    .setURI(plugin.getScm())
                    .setDirectory(repo.toFile())
                    .call()) {
                this.scmRepository = Paths.get(
                        git.getRepository().getDirectory().getParentFile().toURI());
            } catch (GitAPIException e) {
                LOGGER.warn("Could not clone Git repository for plugin {}", pluginName, e);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not create temporary folder for plugin {}", pluginName, e);
        }
    }

    public Optional<Path> getScmRepository() {
        return Optional.ofNullable(scmRepository);
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

    public void setPluginDocumentationLinks(Map<String, String> pluginDocumentationLinks) {
        this.pluginDocumentationLinks = pluginDocumentationLinks;
    }

    public Map<String, String> getPluginDocumentationLinks() {
        return pluginDocumentationLinks;
    }

    /**
     * Returns the GitHub repository of the plugin source code.
     * This needs to be in the format 'organization/repository' for the GitHub.
     * Returns empty if the scm link of the plugin is not set to point to jenkinsci organization.
     *
     * @return the GitHub repository of the plugin source code or empty if not formatted correctly
     */
    public Optional<String> getRepositoryName() {
        if (plugin.getScm() == null || plugin.getScm().isBlank()) {
            return Optional.empty();
        }
        final Matcher match = SCMLinkValidationProbe.GH_PATTERN.matcher(plugin.getScm());
        return match.find() ? Optional.of(match.group("repo")) : Optional.empty();
    }

    public Optional<Path> getScmFolderPath() {
        return Optional.ofNullable(scmFolderPath);
    }

    public void setScmFolderPath(Path scmFolderPath) {
        this.scmFolderPath = scmFolderPath;
    }

    /* default */ void cleanUp() throws IOException {
        if (scmRepository != null) {
            try (Stream<Path> paths = Files.walk(this.scmRepository)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }
}
