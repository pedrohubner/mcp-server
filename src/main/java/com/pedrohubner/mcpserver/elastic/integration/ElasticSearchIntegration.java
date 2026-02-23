package com.pedrohubner.mcpserver.elastic.integration;

import com.pedrohubner.mcpserver.common.config.integration.RestClientStatusHandler;
import com.pedrohubner.mcpserver.elastic.dto.ElasticCountResponse;
import com.pedrohubner.mcpserver.elastic.dto.ElasticIndexInfo;
import com.pedrohubner.mcpserver.elastic.dto.ElasticSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ElasticSearchIntegration {
    private static final String RESULT = "result";
    private static final String SUCCESS = "success";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";
    private static final String INDEX_PATTERN = "indexPattern";

    private final RestClient elasticSearchRestClient;

    public Map<String, Object> searchLogs(
            String indexPattern, Map<String, Object> requestBody
    ) {
        final var response = elasticSearchRestClient.post()
                .uri("/" + indexPattern + "/_search")
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(ElasticSearchResponse.class);

        return Optional.ofNullable(response)
                .map(elasticResponse -> Map.of(
                        SUCCESS, true,
                        INDEX_PATTERN, indexPattern,
                        REQUEST, requestBody,
                        RESPONSE, response
                ))
                .orElseGet(Collections::emptyMap);
    }

    public Map<String, Object> countLogs(String indexPattern, Map<String, Object> requestBody) {
        final var response = elasticSearchRestClient.post()
                .uri("/" + indexPattern + "/_count")
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(ElasticCountResponse.class);

        return Optional.ofNullable(response)
                .map(elasticResponse -> Map.of(
                        SUCCESS, true,
                        INDEX_PATTERN, indexPattern,
                        REQUEST, requestBody,
                        RESPONSE, response
                ))
                .orElseGet(Collections::emptyMap);
    }

    public Map<String, Object> listIndices(String indexPattern) {
        final var response = elasticSearchRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/_cat/indices/{indexPattern}")
                        .queryParam("format", "json")
                        .queryParam("s", "index")
                        .build(indexPattern))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(new ParameterizedTypeReference<List<ElasticIndexInfo>>() {
                });

        return Map.of(
                SUCCESS, true,
                INDEX_PATTERN, indexPattern,
                RESPONSE, response
        );
    }

    public Map<String, Object> resolveIndices(String indexPattern) {
        final var response = elasticSearchRestClient.get()
                .uri("/_resolve/index/" + indexPattern)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        return Map.of(
                SUCCESS, true,
                INDEX_PATTERN, indexPattern,
                RESPONSE, response
        );
    }

    public Map<String, Object> fieldCapabilities(
            String indexPattern, Map<String, Object> requestBody, Boolean includeUnmapped
    ) {
        final var response = elasticSearchRestClient.post()
                .uri(uriBuilder -> {
                    final var builder = uriBuilder.path("/{indexPattern}/_field_caps");
                    if (Boolean.TRUE.equals(includeUnmapped)) {
                        builder.queryParam("include_unmapped", true);
                    }
                    return builder.build(indexPattern);
                })
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        return Map.of(
                SUCCESS, true,
                INDEX_PATTERN, indexPattern,
                REQUEST, requestBody,
                RESPONSE, response
        );
    }

    public Map<String, Object> termsEnum(String indexPattern, Map<String, Object> requestBody) {
        final var response = elasticSearchRestClient.post()
                .uri("/{indexPattern}/_terms_enum", indexPattern)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        return Map.of(
                SUCCESS, true,
                INDEX_PATTERN, indexPattern,
                REQUEST, requestBody,
                RESPONSE, response
        );
    }

    public Map<String, Object> aggregateLogs(String indexPattern, Map<String, Object> requestBody) {
        final var response = elasticSearchRestClient.post()
                .uri("/{indexPattern}/_search", indexPattern)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        return Map.of(
                SUCCESS, true,
                INDEX_PATTERN, indexPattern,
                REQUEST, requestBody,
                RESPONSE, response
        );
    }

    public Map<String, Object> multiSearch(String indexPattern, String requestBody, int queryCount) {
        final var response = elasticSearchRestClient.post()
                .uri("/{indexPattern}/_msearch", indexPattern)
                .contentType(MediaType.valueOf("application/x-ndjson"))
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        return Map.of(
                SUCCESS, true,
                INDEX_PATTERN, indexPattern,
                "queryCount", queryCount,
                RESPONSE, response
        );
    }

    public Map<String, Object> submitAsyncSearch(
            String indexPattern, Map<String, Object> requestBody,
            String waitForCompletionTimeout, String keepAlive
    ) {
        final var response = elasticSearchRestClient.post()
                .uri(uriBuilder -> {
                    final var builder = uriBuilder.path("/{indexPattern}/_async_search");
                    if (StringUtils.hasText(waitForCompletionTimeout)) {
                        builder.queryParam("wait_for_completion_timeout", waitForCompletionTimeout.trim());
                    }
                    if (StringUtils.hasText(keepAlive)) {
                        builder.queryParam("keep_alive", keepAlive.trim());
                    }
                    return builder.build(indexPattern);
                })
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(INDEX_PATTERN, indexPattern);
        metadata.put(REQUEST, requestBody);
        if (StringUtils.hasText(waitForCompletionTimeout)) {
            metadata.put("waitForCompletionTimeout", waitForCompletionTimeout.trim());
        }
        if (StringUtils.hasText(keepAlive)) {
            metadata.put("keepAlive", keepAlive.trim());
        }
        metadata.put(RESPONSE, response);

        return Map.of(
                SUCCESS, true,
                "metadata", metadata
        );
    }

    public Map<String, Object> getAsyncSearch(String asyncSearchId, String keepAlive) {
        final var response = elasticSearchRestClient.get()
                .uri(uriBuilder -> {
                    final var builder = uriBuilder.path("/_async_search/{asyncSearchId}");
                    if (StringUtils.hasText(keepAlive)) {
                        builder.queryParam("keep_alive", keepAlive.trim());
                    }
                    return builder.build(asyncSearchId);
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("asyncSearchId", asyncSearchId);
        if (StringUtils.hasText(keepAlive)) {
            payload.put("keepAlive", keepAlive.trim());
        }
        payload.put(RESPONSE, response);

        return Map.of(
                SUCCESS, true,
                RESULT, payload
        );
    }

    public Map<String, Object> validateQuery(
            String indexPattern, Map<String, Object> requestBody, Boolean explain, Boolean allShards
    ) {
        final var response = elasticSearchRestClient.post()
                .uri(uriBuilder -> {
                    final var builder = uriBuilder.path("/{indexPattern}/_validate/query");
                    if (Boolean.TRUE.equals(explain)) {
                        builder.queryParam("explain", true);
                    }
                    if (Boolean.TRUE.equals(allShards)) {
                        builder.queryParam("all_shards", true);
                    }
                    return builder.build(indexPattern);
                })
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(INDEX_PATTERN, indexPattern);
        metadata.put(REQUEST, requestBody);
        if (Boolean.TRUE.equals(explain)) {
            metadata.put("explain", true);
        }
        if (Boolean.TRUE.equals(allShards)) {
            metadata.put("allShards", true);
        }
        metadata.put(RESPONSE, response);

        return Map.of(
                SUCCESS, true,
                RESULT, metadata
        );
    }

    public Map<String, Object> clusterHealth(
            String indexPattern, String level, String waitForStatus, String timeout
    ) {
        final var response = elasticSearchRestClient.get()
                .uri(uriBuilder -> {
                    final String resolvedPath = StringUtils.hasText(indexPattern) ?
                            "/_cluster/health/{indexPattern}" : "/_cluster/health";
                    final var builder = uriBuilder.path(resolvedPath);
                    if (StringUtils.hasText(level)) {
                        builder.queryParam("level", level.trim());
                    }
                    if (StringUtils.hasText(waitForStatus)) {
                        builder.queryParam("wait_for_status", waitForStatus.trim());
                    }
                    if (StringUtils.hasText(timeout)) {
                        builder.queryParam("timeout", timeout.trim());
                    }
                    return StringUtils.hasText(indexPattern) ?
                            builder.build(indexPattern.trim()) : builder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        final Map<String, Object> metadata = new LinkedHashMap<>();
        if (StringUtils.hasText(indexPattern)) {
            metadata.put(INDEX_PATTERN, indexPattern.trim());
        }
        if (StringUtils.hasText(level)) {
            metadata.put("level", level.trim());
        }
        if (StringUtils.hasText(waitForStatus)) {
            metadata.put("waitForStatus", waitForStatus.trim());
        }
        if (StringUtils.hasText(timeout)) {
            metadata.put("timeout", timeout.trim());
        }
        metadata.put(RESPONSE, response);

        return Map.of(
                SUCCESS, true,
                RESULT, metadata
        );
    }

    public Map<String, Object> indexStats(String indexPattern, String metrics) {
        final var response = elasticSearchRestClient.get()
                .uri(uriBuilder -> {
                    if (StringUtils.hasText(metrics)) {
                        return uriBuilder.path("/{indexPattern}/_stats/{metrics}")
                                .build(indexPattern, metrics.trim());
                    }
                    return uriBuilder.path("/{indexPattern}/_stats")
                            .build(indexPattern);
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, RestClientStatusHandler.handle4xxError())
                .onStatus(HttpStatusCode::is5xxServerError, RestClientStatusHandler.handle5xxError())
                .body(Object.class);

        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(INDEX_PATTERN, indexPattern);
        if (StringUtils.hasText(metrics)) {
            metadata.put("metrics", metrics.trim());
        }
        metadata.put(RESPONSE, response);

        return Map.of(
                SUCCESS, true,
                RESULT, metadata
        );
    }
}
