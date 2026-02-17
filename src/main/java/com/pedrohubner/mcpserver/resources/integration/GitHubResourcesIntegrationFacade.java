package com.pedrohubner.mcpserver.resources.integration;

import com.pedrohubner.mcpserver.resources.dto.GitHubRepoTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubResourcesIntegrationFacade {
    private final GitHubResourcesIntegration integration;

    public List<GitHubRepoTree> getFiles() {
        final var repoTree = integration.getRepoTree();
        if (Objects.isNull(repoTree))
            return Collections.emptyList();

        if (Boolean.TRUE.equals(repoTree.truncated()))
            log.warn("GitHub tree retornou truncada para resources.");

        return repoTree.safeTree().stream()
                .filter(GitHubRepoTree::isInsideResourcesDirectory)
                .toList();
    }
}
