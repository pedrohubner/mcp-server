package com.pedrohubner.mcpserver.resources;

import com.pedrohubner.mcpserver.resources.dto.GitHubRepoTree;
import com.pedrohubner.mcpserver.resources.integration.ResourcesGitLabIntegration;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Configuration
public class ResourcesProvider {

    @Bean
    public List<SyncResourceSpecification> gitLabResources(ResourcesGitLabIntegration integration) {
        final var files = Optional.ofNullable(integration.listFiles()).orElseGet(List::of);
        final var specs = files.stream()
                .filter(this::isMarkdownFile)
                .map(item -> buildResourceSpec(integration, item))
                .toList();

        log.info("Registrados {} resources markdown do GitHub", specs.size());
        return specs;
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
                .uri("github://resources/" + item.name())
                .name(item.name())
                .description("Resource: " + item.name())
                .mimeType("text/markdown")
                .build();
    }

    private McpSchema.@NonNull ReadResourceResult buildResourceResult(ResourcesGitLabIntegration integration, GitHubRepoTree item, McpSchema.ReadResourceRequest request) {
        log.info("MCP resource chamado: name={}, uri={}", item.name(), request.uri());
        String content = integration.getFileContent(item.name());
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(request.uri(), "text/markdown", content))
        );
    }
}
