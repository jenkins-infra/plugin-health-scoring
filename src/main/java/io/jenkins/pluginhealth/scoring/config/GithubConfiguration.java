package io.jenkins.pluginhealth.scoring.config;

import java.io.IOException;
import javax.validation.constraints.NotBlank;

import org.kohsuke.github.GitHub;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "github")
@ConstructorBinding
@Validated
public final class GithubConfiguration {
    @NotBlank private final String oauth;

    public GithubConfiguration(String oauth) {
        this.oauth = oauth;
    }

    @Bean
    public GitHub getGithub() throws IOException {
        return GitHub.connectUsingOAuth(oauth);
    }
}
