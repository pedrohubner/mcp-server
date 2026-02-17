package com.pedrohubner.mcpserver.resources.dto;

public record GitHubRepoTree(
        String name,
        String path,
        String type
) {
}
