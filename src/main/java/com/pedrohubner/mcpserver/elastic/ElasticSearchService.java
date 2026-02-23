package com.pedrohubner.mcpserver.elastic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedrohubner.mcpserver.elastic.integration.ElasticSearchIntegration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElasticSearchService {
    private static final int MAX_SIZE = 200;
    private static final int MAX_TERMS_SIZE = 1000;
    private static final int MAX_TOP_N = 200;
    private static final int DEFAULT_SIZE = 20;
    private static final int DEFAULT_TOP_N = 10;
    private static final String DEFAULT_QUERY = "*";
    private static final String DEFAULT_FIELDS = "*";
    private static final String DEFAULT_INDEX_PATTERN = "mcp-server-logs-*";
    private static final String DEFAULT_AGGREGATION_TYPE = "date_histogram";
    private static final String DEFAULT_HISTOGRAM_INTERVAL = "1h";
    private static final String DEFAULT_WAIT_FOR_COMPLETION_TIMEOUT = "1s";
    private static final String DEFAULT_KEEP_ALIVE = "1m";
    private static final String DEFAULT_CLUSTER_LEVEL = "cluster";
    private static final String DEFAULT_CLUSTER_TIMEOUT = "30s";

    private static final List<String> SUPPORTED_AGGREGATION_TYPES = List.of(
            "date_histogram", "terms", "cardinality"
    );
    private static final List<String> SUPPORTED_CLUSTER_LEVELS = List.of(
            "cluster", "indices", "shards"
    );

    private final ElasticSearchIntegration elasticSearchIntegration;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> searchLogs(
            String indexPattern, String query, Integer size, String fromTimestamp, String toTimestamp
    ) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedQuery = this.normalizeQuery(query);
        final var normalizedSize = this.normalizeSize(size);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("size", normalizedSize);
        requestBody.put("sort", List.of(Map.of("@timestamp", Map.of("order", "desc"))));
        requestBody.put("query", buildQuery(normalizedQuery, fromTimestamp, toTimestamp));

        return elasticSearchIntegration.searchLogs(
                normalizedIndexPattern, requestBody
        );
    }

    public Map<String, Object> countLogs(String indexPattern, String query, String fromTimestamp, String toTimestamp) {
        final var normalizedQuery = this.normalizeQuery(query);
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final Map<String, Object> requestBody = Map.of(
                "query", this.buildQuery(normalizedQuery, fromTimestamp, toTimestamp)
        );

        return elasticSearchIntegration.countLogs(normalizedIndexPattern, requestBody);
    }

    public Map<String, Object> listIndices(String indexPattern) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        return elasticSearchIntegration.listIndices(normalizedIndexPattern);
    }

    public Map<String, Object> fieldCapabilities(String indexPattern, String fields, Boolean includeUnmapped) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedFields = this.normalizeFields(fields);
        final Map<String, Object> requestBody = Map.of("fields", normalizedFields);
        return elasticSearchIntegration.fieldCapabilities(normalizedIndexPattern, requestBody, includeUnmapped);
    }

    public Map<String, Object> termsEnum(
            String indexPattern, String field, String prefix, Integer size, String query,
            String fromTimestamp, String toTimestamp, Boolean caseInsensitive
    ) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedField = this.normalizeRequiredText(field, "field");
        final var normalizedPrefix = this.normalizeOptional(prefix);
        final var normalizedSize = this.normalizeBoundedSize(size, DEFAULT_SIZE, MAX_TERMS_SIZE);

        final Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("field", normalizedField);
        requestBody.put("size", normalizedSize);
        if (StringUtils.hasText(normalizedPrefix)) {
            requestBody.put("string", normalizedPrefix);
        }
        if (caseInsensitive != null) {
            requestBody.put("case_insensitive", caseInsensitive);
        }
        if (this.shouldApplyFilters(query, fromTimestamp, toTimestamp)) {
            requestBody.put(
                    "index_filter",
                    this.buildQuery(this.normalizeQuery(query), fromTimestamp, toTimestamp)
            );
        }

        return elasticSearchIntegration.termsEnum(normalizedIndexPattern, requestBody);
    }

    public Map<String, Object> aggregateLogs(
            String indexPattern, String query, String fromTimestamp, String toTimestamp,
            String aggregationType, String aggregationField, String interval, Integer topN
    ) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedQuery = this.normalizeQuery(query);
        final var normalizedAggregationType = this.normalizeAggregationType(aggregationType);

        final Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("size", 0);
        requestBody.put("query", this.buildQuery(normalizedQuery, fromTimestamp, toTimestamp));
        requestBody.put("aggs", this.buildAggregation(
                normalizedAggregationType, aggregationField, interval, topN
        ));

        return elasticSearchIntegration.aggregateLogs(normalizedIndexPattern, requestBody);
    }

    public Map<String, Object> multiSearch(
            String indexPattern, List<String> queries, Integer size, String fromTimestamp, String toTimestamp
    ) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedSize = this.normalizeSize(size);
        final var normalizedQueries = this.normalizeQueries(queries);
        final var requestBody = this.buildMultiSearchRequestBody(
                normalizedIndexPattern, normalizedQueries, normalizedSize, fromTimestamp, toTimestamp
        );

        return elasticSearchIntegration.multiSearch(normalizedIndexPattern, requestBody, normalizedQueries.size());
    }

    public Map<String, Object> submitAsyncSearch(
            String indexPattern, String query, Integer size, String fromTimestamp, String toTimestamp,
            String waitForCompletionTimeout, String keepAlive
    ) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedQuery = this.normalizeQuery(query);
        final var normalizedSize = this.normalizeSize(size);
        final var normalizedWaitForCompletionTimeout = this.normalizeDuration(
                waitForCompletionTimeout, DEFAULT_WAIT_FOR_COMPLETION_TIMEOUT
        );
        final var normalizedKeepAlive = this.normalizeDuration(keepAlive, DEFAULT_KEEP_ALIVE);

        final Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("size", normalizedSize);
        requestBody.put("sort", List.of(Map.of("@timestamp", Map.of("order", "desc"))));
        requestBody.put("query", this.buildQuery(normalizedQuery, fromTimestamp, toTimestamp));

        return elasticSearchIntegration.submitAsyncSearch(
                normalizedIndexPattern, requestBody, normalizedWaitForCompletionTimeout, normalizedKeepAlive
        );
    }

    public Map<String, Object> validateQuery(
            String indexPattern, String query, String fromTimestamp, String toTimestamp,
            Boolean explain, Boolean allShards
    ) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedQuery = this.normalizeQuery(query);
        final boolean normalizedExplain = explain == null || explain;
        final boolean normalizedAllShards = allShards != null && allShards;
        final Map<String, Object> requestBody = Map.of(
                "query", this.buildQuery(normalizedQuery, fromTimestamp, toTimestamp)
        );

        return elasticSearchIntegration.validateQuery(
                normalizedIndexPattern, requestBody, normalizedExplain, normalizedAllShards
        );
    }

    public Map<String, Object> clusterHealth(
            String indexPattern, String level, String waitForStatus, String timeout
    ) {
        final var normalizedIndexPattern = this.normalizeOptional(indexPattern);
        final var normalizedLevel = this.normalizeClusterLevel(level);
        final var normalizedWaitForStatus = this.normalizeOptional(waitForStatus);
        final var normalizedTimeout = this.normalizeDuration(timeout, DEFAULT_CLUSTER_TIMEOUT);
        return elasticSearchIntegration.clusterHealth(
                normalizedIndexPattern, normalizedLevel, normalizedWaitForStatus, normalizedTimeout
        );
    }

    public Map<String, Object> indexStats(String indexPattern, String metrics) {
        final var normalizedIndexPattern = this.normalizeIndexPattern(indexPattern);
        final var normalizedMetrics = this.normalizeOptional(metrics);
        return elasticSearchIntegration.indexStats(normalizedIndexPattern, normalizedMetrics);
    }

    private String normalizeIndexPattern(String indexPattern) {
        if (StringUtils.hasText(indexPattern)) {
            return indexPattern.trim();
        }
        return DEFAULT_INDEX_PATTERN;
    }

    private String normalizeQuery(String query) {
        if (StringUtils.hasText(query)) {
            return query.trim();
        }
        return DEFAULT_QUERY;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    private int normalizeBoundedSize(Integer size, int defaultValue, int maxValue) {
        if (size == null) {
            return defaultValue;
        }
        return Math.max(1, Math.min(size, maxValue));
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' é obrigatório.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private String normalizeDuration(String value, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return defaultValue;
    }

    private List<String> normalizeFields(String fields) {
        if (!StringUtils.hasText(fields)) {
            return List.of(DEFAULT_FIELDS);
        }
        final var normalized = List.of(fields.split(","))
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return List.of(DEFAULT_FIELDS);
        }
        return normalized;
    }

    private String normalizeAggregationType(String aggregationType) {
        if (!StringUtils.hasText(aggregationType)) {
            return DEFAULT_AGGREGATION_TYPE;
        }
        final var normalized = aggregationType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_AGGREGATION_TYPES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "aggregationType inválido. Use um dos valores: " + String.join(", ", SUPPORTED_AGGREGATION_TYPES)
            );
        }
        return normalized;
    }

    private String normalizeClusterLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return DEFAULT_CLUSTER_LEVEL;
        }
        final var normalized = level.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_CLUSTER_LEVELS.contains(normalized)) {
            throw new IllegalArgumentException(
                    "level inválido. Use um dos valores: " + String.join(", ", SUPPORTED_CLUSTER_LEVELS)
            );
        }
        return normalized;
    }

    private List<String> normalizeQueries(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return List.of(DEFAULT_QUERY);
        }
        final var normalized = queries.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return List.of(DEFAULT_QUERY);
        }
        return normalized;
    }

    private boolean shouldApplyFilters(String query, String fromTimestamp, String toTimestamp) {
        return (StringUtils.hasText(query) && !DEFAULT_QUERY.equals(query.trim()))
                || StringUtils.hasText(fromTimestamp)
                || StringUtils.hasText(toTimestamp);
    }

    private Map<String, Object> buildAggregation(
            String aggregationType, String aggregationField, String interval, Integer topN
    ) {
        final Map<String, Object> aggregationDefinition = new LinkedHashMap<>();
        switch (aggregationType) {
            case "date_histogram" -> {
                final String field = StringUtils.hasText(aggregationField) ? aggregationField.trim() : "@timestamp";
                final String fixedInterval = StringUtils.hasText(interval) ? interval.trim() : DEFAULT_HISTOGRAM_INTERVAL;
                aggregationDefinition.put("date_histogram", Map.of(
                        "field", field,
                        "fixed_interval", fixedInterval,
                        "min_doc_count", 0
                ));
            }
            case "terms" -> {
                final String field = this.normalizeRequiredText(aggregationField, "aggregationField");
                final int normalizedTopN = this.normalizeBoundedSize(topN, DEFAULT_TOP_N, MAX_TOP_N);
                aggregationDefinition.put("terms", Map.of(
                        "field", field,
                        "size", normalizedTopN,
                        "order", Map.of("_count", "desc")
                ));
            }
            case "cardinality" -> {
                final String field = this.normalizeRequiredText(aggregationField, "aggregationField");
                aggregationDefinition.put("cardinality", Map.of("field", field));
            }
            default -> throw new IllegalArgumentException(
                    "aggregationType inválido. Use um dos valores: " + String.join(", ", SUPPORTED_AGGREGATION_TYPES)
            );
        }

        return Map.of("aggregation", aggregationDefinition);
    }

    private String buildMultiSearchRequestBody(
            String indexPattern, List<String> queries, int size, String fromTimestamp, String toTimestamp
    ) {
        final var ndjson = new StringBuilder();
        for (String query : queries) {
            final Map<String, Object> header = Map.of("index", indexPattern);
            final Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", size);
            body.put("sort", List.of(Map.of("@timestamp", Map.of("order", "desc"))));
            body.put("query", this.buildQuery(this.normalizeQuery(query), fromTimestamp, toTimestamp));

            ndjson.append(this.toJson(header)).append("\n");
            ndjson.append(this.toJson(body)).append("\n");
        }
        return ndjson.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Erro ao serializar corpo da requisição para o Elasticsearch.", ex);
        }
    }

    private Map<String, Object> buildQuery(String query, String fromTimestamp, String toTimestamp) {
        List<Map<String, Object>> must = new ArrayList<>();

        if (StringUtils.hasText(query) && !DEFAULT_QUERY.equals(query)) {
            must.add(Map.of("query_string", Map.of("query", query)));
        }

        Map<String, Object> range = new LinkedHashMap<>();
        if (StringUtils.hasText(fromTimestamp)) {
            range.put("gte", fromTimestamp.trim());
        }
        if (StringUtils.hasText(toTimestamp)) {
            range.put("lte", toTimestamp.trim());
        }

        if (!range.isEmpty()) {
            must.add(Map.of("range", Map.of("@timestamp", range)));
        }

        if (must.isEmpty()) {
            return Map.of("match_all", Map.of());
        }

        return Map.of("bool", Map.of("must", must));
    }
}
