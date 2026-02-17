package com.pedrohubner.mcpserver.resources;

import com.pedrohubner.mcpserver.resources.dto.GitHubRepoTree;
import com.pedrohubner.mcpserver.resources.integration.ResourcesGitLabIntegration;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ResourcesSpecificationFactory {

    public static final String RESOURCE_URI_PREFIX = "github://resources/";

    public List<SyncResourceSpecification> buildMarkdownResourceSpecs(ResourcesGitLabIntegration integration,
                                                                      List<GitHubRepoTree> files) {
        return files.stream()
                .filter(this::isMarkdownFile)
                .map(item -> this.buildResourceSpec(integration, item))
                .toList();
    }

    private boolean isMarkdownFile(GitHubRepoTree item) {
        return "file".equals(item.type())
                && Objects.nonNull(item.name())
                && item.name().toLowerCase().endsWith(".md");
    }

    private SyncResourceSpecification buildResourceSpec(ResourcesGitLabIntegration integration, GitHubRepoTree item) {
        final var resource = this.buildSchema(item);
        return new SyncResourceSpecification(
                resource,
                (exchange, request) -> this.buildResourceResult(integration, item, request)
        );
    }

    private McpSchema.Resource buildSchema(GitHubRepoTree item) {
        return McpSchema.Resource.builder()
                .uri(RESOURCE_URI_PREFIX + item.name())
                .name(item.name())
                .description("Resource: " + item.name())
                .mimeType("text/markdown")
                .build();
    }

    private McpSchema.@NonNull ReadResourceResult buildResourceResult(ResourcesGitLabIntegration integration,
                                                                      GitHubRepoTree item,
                                                                      McpSchema.ReadResourceRequest request) {
        log.info("MCP resource chamado: name={}, uri={}", item.name(), request.uri());
        String content = integration.getFileContent(item.name());
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(request.uri(), "text/markdown", content))
        );
    }
}
