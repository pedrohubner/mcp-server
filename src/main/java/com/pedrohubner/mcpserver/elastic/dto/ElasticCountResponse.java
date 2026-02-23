package com.pedrohubner.mcpserver.elastic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ElasticCountResponse {
    private Long count;

    @JsonProperty("_shards")
    private Shards shards;

    @Data
    public static class Shards {
        private Integer total;
        private Integer successful;
        private Integer skipped;
        private Integer failed;
    }
}

