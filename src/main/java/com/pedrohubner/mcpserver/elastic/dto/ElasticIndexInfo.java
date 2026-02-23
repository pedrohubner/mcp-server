package com.pedrohubner.mcpserver.elastic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ElasticIndexInfo {
    private String health;
    private String status;
    private String index;
    private String uuid;

    @JsonProperty("pri")
    private String primaryShards;

    @JsonProperty("rep")
    private String replicaShards;

    @JsonProperty("docs.count")
    private String docsCount;

    @JsonProperty("docs.deleted")
    private String docsDeleted;

    @JsonProperty("store.size")
    private String storeSize;

    @JsonProperty("pri.store.size")
    private String primaryStoreSize;
}

