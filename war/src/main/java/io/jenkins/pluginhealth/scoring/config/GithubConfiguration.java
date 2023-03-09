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

package io.jenkins.pluginhealth.scoring.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.stream.Collectors;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.apache.tomcat.util.codec.binary.Base64;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "github")
@Validated
public class GithubConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(GithubConfiguration.class);

    @NotEmpty
    private final String appId;
    private final Path privateKeyPath;
    @NotEmpty
    private final String appInstallationName;
    @Min(value = 5000, message = "TTL should not be shorter than 5_000ms")
    @Max(value = 10 * 60 * 1000, message = "TTL cannot be longer than 10min")
    private final long ttl;

    @ConstructorBinding
    public GithubConfiguration(String appId, Path privateKeyPath, String appInstallationName, @DefaultValue("300000") long ttl) {
        this.appId = appId;
        this.privateKeyPath = privateKeyPath;
        this.appInstallationName = appInstallationName;
        this.ttl = ttl;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("GitHub API Client TTL: {}ms", ttl);
            LOGGER.debug("Private Key Path: {} [valid: {}]", privateKeyPath, Files.exists(privateKeyPath) && Files.isReadable(privateKeyPath));
        }
    }

    public GitHub getGitHub() throws IOException {
        final GitHub ghApp = new GitHubBuilder().withJwtToken(createJWT()).build();
        final GHAppInstallation ghAppInstall = getAppInstallation(ghApp);
        final GHAppInstallationToken ghAppInstallationToken = ghAppInstall.createToken().create();
        final OkHttpClient httpClient = new OkHttpClient.Builder().cache(
            new Cache(Files.createTempDirectory("http_cache").toFile(), 50 * 1024 * 1024)
        ).build();
        return new GitHubBuilder()
            .withConnector(new OkHttpGitHubConnector(httpClient))
            .withAppInstallationToken(ghAppInstallationToken.getToken()).build();
    }

    private GHAppInstallation getAppInstallation(GitHub ghApp) throws IOException {
        final GHApp app = ghApp.getApp();
        try {
            return app.getInstallationByOrganization(appInstallationName);
        } catch (IOException e) {
            return app.getInstallationByUser(appInstallationName);
        }
    }

    private RSAPrivateKey getKey() {
        try {
            final String privateKeyPEM = Files.readAllLines(privateKeyPath, Charset.defaultCharset())
                .stream()
                .filter(line -> !line.startsWith("-----") && !line.endsWith("-----"))
                .collect(Collectors.joining());
            final byte[] encoded = Base64.decodeBase64(privateKeyPEM);
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Could not reconstruct the private key, the given algorithm could not be found");
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Could not reconstruct the private key");
        }
        return null;
    }

    private String createJWT() {
        Algorithm algorithm = Algorithm.RSA256(getKey());
        return JWT.create()
            .withIssuedAt(Instant.now().minusMillis(60 * 1000))  /* 60sec in past for clock drift */
            .withExpiresAt(Instant.now().plusMillis(ttl))
            .withIssuer(appId)
            .sign(algorithm);
    }
}
