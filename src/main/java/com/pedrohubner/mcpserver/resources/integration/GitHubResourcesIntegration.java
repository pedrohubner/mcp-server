package com.pedrohubner.mcpserver.resources.integration;

import com.pedrohubner.mcpserver.common.config.integration.RestClientStatusHandler;
import com.pedrohubner.mcpserver.resources.dto.GitHubRepoTreeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubResourcesIntegration {
    private static final String REPOSITORY_RESOURCES_ROOT = "resources/";

    private final RestClient githubRestClient;

    @Cacheable("resources-list")
    public GitHubRepoTreeResponse getRepoTree() {
        final var uri = "repos/pedrohubner/mcp-resources/git/trees/main?recursive=1";
        return githubRestClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(GitHubRepoTreeResponse.class);
    }

    @Cacheable(value = "resources-content", key = "#resourcePath")
    public String getFileContent(String resourcePath) {
        final var normalizedPath = this.normalizePath(resourcePath);
        final var encodedPath = UriUtils.encodePath(normalizedPath, StandardCharsets.UTF_8);
        final var uri = "repos/pedrohubner/mcp-resources/contents/" + encodedPath + "?ref=main";

        log.debug("Get resource from GitHub - URI: {}, resourcePath: {}", uri, resourcePath);

        return githubRestClient.get()
                .uri(uri)
                .accept(MediaType.valueOf("application/vnd.github.raw"))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(String.class);
    }

    private String normalizePath(String resourcePath) {
        if (!StringUtils.hasText(resourcePath))
            throw new IllegalArgumentException("Resource path must not be blank.");

        if (resourcePath.startsWith(REPOSITORY_RESOURCES_ROOT))
            return resourcePath;

        return REPOSITORY_RESOURCES_ROOT + resourcePath;
    }
}
