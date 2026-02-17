package com.pedrohubner.mcpserver.resources;

import com.pedrohubner.mcpserver.resources.integration.GitHubResourcesIntegrationFacade;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourcesRefreshService {
    private static final String RESOURCES_LIST_CACHE = "resources-list";
    private static final String RESOURCES_CONTENT_CACHE = "resources-content";

    private final ReentrantLock refreshLock = new ReentrantLock();

    private final CacheManager cacheManager;
    private final McpSyncServer mcpSyncServer;
    private final GitHubResourcesIntegrationFacade facade;
    private final ResourcesSpecificationFactory specificationFactory;

    public void refresh(String trigger) {
        if (!refreshLock.tryLock()) {
            log.info("Refresh ignorado, já existe uma execução em andamento. trigger={}", trigger);
            return;
        }

        try {
            this.evictCaches();

            final var files = Optional.ofNullable(facade.getFiles()).orElseGet(List::of);
            final var desiredSpecs = specificationFactory.buildMarkdownResourceSpecs(files);
            final var desiredByUri = this.mapByUri(desiredSpecs);
            final var currentUris = this.currentManagedUris();

            boolean resourcesChanged = false;

            for (String currentUri : currentUris) {
                if (!desiredByUri.containsKey(currentUri)) {
                    mcpSyncServer.removeResource(currentUri);
                    resourcesChanged = true;
                    log.info("Resource removido dinamicamente: {}", currentUri);
                }
            }

            for (var entry : desiredByUri.entrySet()) {
                if (!currentUris.contains(entry.getKey())) {
                    mcpSyncServer.addResource(entry.getValue());
                    resourcesChanged = true;
                    log.info("Resource adicionado dinamicamente: {}", entry.getKey());
                }
            }

            if (resourcesChanged) {
                mcpSyncServer.notifyResourcesListChanged();
                log.info("Refresh concluído com alterações na lista de resources. trigger={}", trigger);
                return;
            }

            log.info("Refresh concluído sem alterações estruturais. trigger={}", trigger);
        } catch (RuntimeException ex) {
            log.error("Erro ao atualizar resources dinamicamente. trigger={}", trigger, ex);
        } finally {
            refreshLock.unlock();
        }
    }

    private void evictCaches() {
        this.evictCache(RESOURCES_LIST_CACHE);
        this.evictCache(RESOURCES_CONTENT_CACHE);
    }

    private void evictCache(String cacheName) {
        final var cache = cacheManager.getCache(cacheName);
        if (Objects.isNull(cache)) {
            log.warn("Cache '{}' não encontrado para limpeza.", cacheName);
            return;
        }
        cache.clear();
    }

    private LinkedHashMap<String, SyncResourceSpecification> mapByUri(List<SyncResourceSpecification> specs) {
        return specs.stream()
                .collect(Collectors.toMap(spec -> spec.resource().uri(),
                        spec -> spec,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Set<String> currentManagedUris() {
        return mcpSyncServer.listResources().stream()
                .map(McpSchema.Resource::uri)
                .filter(uri -> uri.startsWith(ResourcesSpecificationFactory.RESOURCE_URI_PREFIX))
                .collect(Collectors.toSet());
    }
}
