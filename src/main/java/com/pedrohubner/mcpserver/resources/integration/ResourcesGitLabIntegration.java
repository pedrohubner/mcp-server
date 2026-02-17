package com.pedrohubner.mcpserver.resources.integration;

import com.pedrohubner.mcpserver.common.config.integration.RestClientStatusHandler;
import com.pedrohubner.mcpserver.resources.dto.GitHubRepoTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourcesGitLabIntegration {
    private final RestClient gitLabRestClient;

    @Cacheable("resources-list")
    public List<GitHubRepoTree> listFiles() {
        final var uri = "repos/pedrohubner/mcp-resources/contents/resources?ref=main";
        return gitLabRestClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(new ParameterizedTypeReference<>() {});
    }

    @Cacheable(value = "resources-content", key = "#fileName")
    public String getFileContent(String fileName) {
        final var uri = "repos/pedrohubner/mcp-resources/contents/resources/{fileName}?ref=main";

        log.debug("Get resource from GitLab - URI template: {}, fileName: {}", uri, fileName);

        return gitLabRestClient.get()
                .uri(uri, fileName)
                .accept(MediaType.valueOf("application/vnd.github.raw"))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(String.class);
    }
}
