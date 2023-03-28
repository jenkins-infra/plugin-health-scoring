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

import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(DocumentationMigrationProbe.ORDER)
public class DocumentationMigrationProbe extends Probe {
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "documentation-migration";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Map<String, String> pluginDocumentationLinks = context.getPluginDocumentationLinks();
        final String scm = plugin.getScm();
        final String linkDocumentationForPlugin = pluginDocumentationLinks.get(plugin.getName());

        return pluginDocumentationLinks.isEmpty() ?
            ProbeResult.error(key(), "No link to documentation can be confirmed") :
            linkDocumentationForPlugin == null ?
                ProbeResult.failure(key(), "Plugin is not listed in documentation migration source") :
                scm != null && scm.contains(linkDocumentationForPlugin) ?
                    ProbeResult.success(key(), "Documentation is located in the plugin repository") :
                    ProbeResult.failure(key(), "Documentation is not located in the plugin repository");
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Reports if the plugin documentation was migrated from the Wiki to GitHub";
    }

    @Override
    protected boolean requiresRelease() {
        return true;
    }

    @Override
    protected String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY};
    }
}
