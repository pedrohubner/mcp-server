package com.pedrohubner.mcpserver.resources.dto;

import java.util.List;
import java.util.Objects;

public record GitHubRepoTreeResponse(
        List<GitHubRepoTree> tree,
        Boolean truncated
) {
    public List<GitHubRepoTree> safeTree() {
        if (Objects.isNull(tree)) {
            return List.of();
        }
        return tree;
    }
}
