package com.pedrohubner.mcpserver.elastic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ElasticSearchResponse {
    private Long took;

    @JsonProperty("timed_out")
    private Boolean timedOut;

    @JsonProperty("_shards")
    private Shards shards;

    private Hits hits;

    @Data
    public static class Shards {
        private Integer total;
        private Integer successful;
        private Integer skipped;
        private Integer failed;
    }

    @Data
    public static class Hits {
        private Total total;

        @JsonProperty("max_score")
        private Double maxScore;

        private List<Hit> hits;

        @Data
        public static class Total {
            private Long value;
            private String relation;
        }

        @Data
        public static class Hit {
            @JsonProperty("_index")
            private String index;

            @JsonProperty("_id")
            private String id;

            @JsonProperty("_score")
            private Double score;

            @JsonProperty("_source")
            private Map<String, Object> source;

            private List<Object> sort;
        }
    }
}

