package com.pedrohubner.mcpserver.resources;

import com.pedrohubner.mcpserver.resources.dto.GitHubRepoTree;
import com.pedrohubner.mcpserver.resources.integration.GitHubResourcesIntegration;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourcesSpecificationFactory {
    public static final String RESOURCE_URI_PREFIX = "github://resources/";

    private final GitHubResourcesIntegration integration;

    public List<SyncResourceSpecification> buildMarkdownResourceSpecs(List<GitHubRepoTree> files) {
        return files.stream()
                .filter(this::isMarkdownFile)
                .map(this::buildResourceSpec)
                .toList();
    }

    private boolean isMarkdownFile(GitHubRepoTree item) {
        return item.isMarkdownFile();
    }

    private SyncResourceSpecification buildResourceSpec(GitHubRepoTree item) {
        final var resource = this.buildSchema(item);
        return new SyncResourceSpecification(
                resource,
                (exchange, request) -> this.buildResourceResult(item, request)
        );
    }

    private McpSchema.Resource buildSchema(GitHubRepoTree item) {
        final var resourcePath = item.relativePathFromResources();
        final var fileName = item.fileName();

        return McpSchema.Resource.builder()
                .uri(RESOURCE_URI_PREFIX + resourcePath)
                .name(fileName)
                .description("Resource: " + resourcePath)
                .mimeType("text/markdown")
                .build();
    }

    private McpSchema.@NonNull ReadResourceResult buildResourceResult(
            GitHubRepoTree item, McpSchema.ReadResourceRequest request
    ) {
        final var resourcePath = item.relativePathFromResources();
        log.info("MCP resource chamado: path={}, uri={}", resourcePath, request.uri());
        String content = integration.getFileContent(resourcePath);
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(request.uri(), "text/markdown", content))
        );
    }
}
