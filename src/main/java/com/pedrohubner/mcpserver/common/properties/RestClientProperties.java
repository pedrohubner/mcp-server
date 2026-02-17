package com.pedrohubner.mcpserver.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rest-client")
public class RestClientProperties {
    private GitHubProperties github = new GitHubProperties();

    @Getter
    @Setter
    public static class GitHubProperties {
        private String url;
        private String token;
    }
}
