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
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Using the analysis done by {@link SCMLinkValidationProbe},
 * this probe will find number of unreleased commits in a repository
 */
@Component
@Order(value = HasUnreleasedProductionChangesProbe.ORDER)
public class HasUnreleasedProductionChangesProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "unreleased-production-changes";

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return ProbeResult.error(key(), "There is no local repository for plugin " + plugin.getName() + ".", this.getVersion());
        }

        final Path repo = context.getScmRepository().get();
        final Optional<Path> folder = context.getScmFolderPath();
        final Set<String> files = new HashSet<>();

        final List<String> paths = new ArrayList<>(3);
        paths.add("pom.xml");

        if (folder.isPresent()) {
            paths.add(folder.get() + "/pom.xml");
            paths.add(folder.get() + "/src/main");
        } else {
            paths.add("src/main");
        }

        try (Git git = Git.open(repo.toFile())) {
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
                ProbeResult.success(KEY, "All production modifications were released.", this.getVersion()) :
                ProbeResult.success(KEY, "Unreleased production modifications might exist in the plugin source code at "
                    + files.stream().sorted(Comparator.naturalOrder()).collect(Collectors.joining(", ")), this.getVersion());
        } catch (IOException | GitAPIException ex) {
            return ProbeResult.error(KEY, ex.getMessage(), this.getVersion());
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
         * ProbeEngine, it must be `false`.
         */
        return false;
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
