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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
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
public class HasUnreleasedProductionChangesProbe  extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(HasUnreleasedProductionChangesProbe.class);

    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "unreleased-production-changes";

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        List<String> productionPathsToCheckForCommits = new ArrayList<>();
        Set<String> filesModifiedAfterRelease  = new HashSet<>();

        final Matcher matcher = SCMLinkValidationProbe.GH_PATTERN.matcher(plugin.getScm());
        if (!matcher.find()) {
            return ProbeResult.failure(key(), "The SCM link is not valid");
        }
        final String folder = matcher.group("folder");

        try (Git git = Git.init().setDirectory(context.getScmRepository().toFile()).call()) {
            final LogCommand logCommand = git.log();

            if (folder != null) {
                productionPathsToCheckForCommits.add(folder + "pom.xml");
                productionPathsToCheckForCommits.add(folder + "src/main");
            }

            productionPathsToCheckForCommits.add("pom.xml");
            productionPathsToCheckForCommits.add("src/main");

            productionPathsToCheckForCommits.forEach(logCommand:: addPath);
            Iterable<RevCommit> commits = logCommand.call();

            for (RevCommit commit : commits) {
                Instant instant = Instant.ofEpochSecond(commit.getCommitTime());
                ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());

                if (commit.getParentCount() > 0) {
                    /*
                    *  if a previous commit exists, compare the difference
                    * */

                    ObjectId oldCommit = git.getRepository().resolve("HEAD^");
                    ObjectId newCommit = git.getRepository().resolve("HEAD");
                    ObjectReader reader = git.getRepository().newObjectReader();
                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(reader, oldCommit);
                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(reader, newCommit);

                    DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream());
                    df.setRepository(git.getRepository());
                    List<DiffEntry> entries = df.scan(oldTreeIter, newTreeIter);

                    for (DiffEntry entry : entries) {
                        if (zonedDateTime.isAfter(plugin.getReleaseTimestamp())) {
                            filesModifiedAfterRelease.add(entry.getNewPath());
                        }
                    }
                }
                else {
                    ObjectId treeId = commit.getTree().getId();
                    TreeWalk treeWalk = new TreeWalk(git.getRepository());
                    treeWalk.addTree(treeId);
                    treeWalk.setRecursive(true);

                    while (treeWalk.next()) {
                        if (treeWalk.isSubtree()) {
                            treeWalk.enterSubtree();
                        }
                        else {
                            if (zonedDateTime.isAfter(plugin.getReleaseTimestamp())) {
                                System.out.println(zonedDateTime + " " + treeWalk.getPathString());
                                filesModifiedAfterRelease.add(treeWalk.getPathString());
                            }
                        }
                    }
                }
            }

            List<String> list = new ArrayList<>(filesModifiedAfterRelease);
            Collections.sort(list);

            ProbeResult result = filesModifiedAfterRelease.isEmpty() ?
                                ProbeResult.success(key(), "All production modifications were released.") :
                                ProbeResult.failure(key(), "Unreleased production modifications might exist in the plugin source code at "
                                    +  String.join(",", list));

            return result;
        } catch (GitAPIException ex) {
            LOGGER.error("There was an issue while cloning the plugin repository", ex);
            return ProbeResult.error(key(), "Could not clone the plugin repository");
        } catch (CorruptObjectException e) {
            throw new RuntimeException(e);
        } catch (IncorrectObjectTypeException e) {
            throw new RuntimeException(e);
        } catch (MissingObjectException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
