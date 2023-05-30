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

import static io.jenkins.pluginhealth.scoring.probes.SCMLinkValidationProbe.GH_PATTERN;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Using the analysis done by {@link SCMLinkValidationProbe},
 * this probe will find number of unreleased commits in a repository
 */
@Component
@Order(value = HasUnreleasedProductionChangesProbe.ORDER)
public class HasUnreleasedProductionChangesProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(HasUnreleasedProductionChangesProbe.class);

    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "unreleased-production-changes";

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        Matcher matcher = GH_PATTERN.matcher(plugin.getScm());
        if (!matcher.find()) {
            return ProbeResult.error(key(), "SCM link doesn't match GitHub plugin repositories");
        }

        final String folder = matcher.group("folder");
        final Set<String> files = new HashSet<>();

        final List<String> paths = new ArrayList<>(3);
        paths.add("pom.xml");
        if (folder != null) {
            paths.add(folder + "/pom.xml");
            paths.add(folder + "/src/main");
        } else {
            paths.add("src/main");
        }

        try (Git git = Git.open(context.getScmRepository().toFile())) {
            LogCommand logCommand = git.log();
            paths.forEach(logCommand::addPath);
            for (RevCommit revCommit : logCommand.call()) {
                Instant commitInstant = revCommit.getCommitterIdent().getWhenAsInstant();
                if (commitInstant.isBefore(plugin.getReleaseTimestamp().toInstant())) {
                    break;
                }

                if (revCommit.getParentCount() > 0) {
                    RevCommit parent = revCommit.getParent(0);
                    DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
                    diffFormatter.setRepository(git.getRepository());
                    diffFormatter.scan(parent.getTree(), revCommit.getTree())
                        .stream()
                        .map(diffEntry -> diffEntry.getPath(DiffEntry.Side.NEW))
                        .filter(s -> paths.stream().anyMatch(s::startsWith))
                        .forEach(files::add);

                } else {
                    TreeWalk treeWalk = new TreeWalk(git.getRepository());
                    treeWalk.addTree(revCommit.getTree());
                    treeWalk.setRecursive(true);

                    while (treeWalk.next()) {
                        String path = treeWalk.getPathString();
                        for (String s : paths) {
                            if (path.startsWith(s)) {
                                files.add(path);
                            }
                        }
                    }
                }
            }

            return files.isEmpty() ?
                ProbeResult.success(KEY, "All production modifications were released.") :
                ProbeResult.failure(KEY, "Unreleased production modifications might exist in the plugin source code at "
                    + files.stream().sorted(Comparator.naturalOrder()).collect(Collectors.joining(", ")));
        } catch (IOException | GitAPIException ex) {
            return ProbeResult.error(KEY, ex.getMessage());
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Register the last commit date on the official plugin repository";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        /*
         * This is counter intuitive, but this probe needs to be executed all the time.
         * So even if the probe seems to be related to code, in order to not be skipped by the
         * ProbeEngine, is must be `false`.
         */
        return false;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY};
    }
}
