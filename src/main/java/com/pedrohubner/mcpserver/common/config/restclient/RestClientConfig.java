package com.pedrohubner.mcpserver.common.config.restclient;

import com.pedrohubner.mcpserver.common.properties.RestClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final RestClientProperties properties;

    @Bean
    public RestClient githubRestClient() {
        return RestClient.builder()
                .baseUrl(properties.getGithub().getUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getGithub().getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public RestClient elasticSearchRestClient() {
        return RestClient.builder()
                .baseUrl(properties.getElasticsearch().getUrl())
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
