package com.pedrohubner.mcpserver.common.config.integration;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@UtilityClass
public class RestClientStatusHandler {

    public static RestClient.ResponseSpec.ErrorHandler handle4xxError() {
        return (request, response) -> {
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Recurso não encontrado - URI: {}", request.getURI());
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND,
                        "Recurso não encontrado: " + request.getURI());
            }
            log.error("Erro 4xx na requisição - Status: {}, URI: {}",
                    response.getStatusCode(), request.getURI());
            throw new RuntimeException("Erro na requisição: " + response.getStatusCode());
        };
    }

    public static RestClient.ResponseSpec.ErrorHandler handle5xxError() {
        return (request, response) -> {
            log.error("Erro 5xx do servidor - Status: {}, URI: {}",
                    response.getStatusCode(), request.getURI());
            throw new RuntimeException("Erro no servidor: " + response.getStatusCode());
        };
    }
}
