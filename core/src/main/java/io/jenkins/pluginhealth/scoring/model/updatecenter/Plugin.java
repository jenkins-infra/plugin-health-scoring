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

package io.jenkins.pluginhealth.scoring.model.updatecenter;

import java.time.ZonedDateTime;
import java.util.List;

import hudson.util.VersionNumber;

public record Plugin(String name, VersionNumber version, String scm,
                     ZonedDateTime releaseTimestamp, List<String> labels,
                     int popularity, String requiredCore, String defaultBranch, List<IssueTrackers> issueTrackers) {
    public io.jenkins.pluginhealth.scoring.model.Plugin toPlugin() {
        return new io.jenkins.pluginhealth.scoring.model.Plugin(this.name(), this.version(), this.scm(), this.releaseTimestamp());
    }

    public List<IssueTrackers> getIssueTrackers() {
        return this.issueTrackers;
    }

    /**
     * Gets issue tracker details about a plugin.
     *
     * @param type The type of platform used to track issues. For ex: GitHub, JIRA
     * @param reportUrl An url to report issues about the plugin
     * @param viewUrl An url to view all the issues in the plugin
     */
    public record IssueTrackers(String type, String viewUrl, String reportUrl){}
}
