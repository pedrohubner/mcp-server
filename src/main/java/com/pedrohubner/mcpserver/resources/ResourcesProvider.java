package com.pedrohubner.mcpserver.resources;

import com.pedrohubner.mcpserver.resources.dto.GitHubRepoTree;
import com.pedrohubner.mcpserver.resources.integration.GitHubResourcesIntegrationFacade;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
public class ResourcesProvider {

    @Bean
    public List<SyncResourceSpecification> githubResources(
            GitHubResourcesIntegrationFacade facade, ResourcesSpecificationFactory specificationFactory
    ) {
        final List<GitHubRepoTree> files;
        try {
            files = Optional.ofNullable(facade.getFiles()).orElseGet(List::of);
        } catch (RuntimeException ex) {
            log.error("Falha ao carregar resources no startup. A aplicação seguirá sem resources iniciais.", ex);
            return List.of();
        }
        final var specs = specificationFactory.buildMarkdownResourceSpecs(files);

        log.info("Registrados {} resources markdown do GitHub", specs.size());
        return specs;
    }
}
