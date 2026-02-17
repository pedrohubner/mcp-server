package com.pedrohubner.mcpserver.resources.dto;

import java.util.Locale;
import java.util.Objects;

public record GitHubRepoTree(
        String path,
        String type
) {
    private static final String RESOURCES_ROOT_PATH = "resources";

    public boolean isInsideResourcesDirectory() {
        return Objects.nonNull(path) && (RESOURCES_ROOT_PATH.equals(path) || path.startsWith(RESOURCES_ROOT_PATH + "/"));
    }

    public boolean isMarkdownFile() {
        return isFileType(type) && Objects.nonNull(path) && path.toLowerCase(Locale.ROOT).endsWith(".md");
    }

    public String relativePathFromResources() {
        if (Objects.isNull(path)) {
            return null;
        }

        if (path.startsWith(RESOURCES_ROOT_PATH + "/")) {
            return path.substring((RESOURCES_ROOT_PATH + "/").length());
        }

        return path;
    }

    public String fileName() {
        final var relativePath = this.relativePathFromResources();
        if (Objects.isNull(relativePath)) {
            return null;
        }

        final int slashIndex = relativePath.lastIndexOf('/');
        if (slashIndex < 0) {
            return relativePath;
        }

        return relativePath.substring(slashIndex + 1);
    }

    private boolean isFileType(String fileType) {
        return "blob".equals(fileType) || "file".equals(fileType);
    }
}
