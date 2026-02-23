package com.pedrohubner.mcpserver.elastic;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@Order(10)
@ConditionalOnProperty(name = "stub.logs.enabled", havingValue = "true")
public class StubLogsBootstrap implements ApplicationRunner {

    // --- cenários: elastic_search_logs, elastic_count_logs, elastic_terms_enum ---
    private static final List<String> SERVICES = List.of(
            "checkout-service",
            "delivery-service",
            "catalog-service",
            "payment-service",
            "mcp-server"
    );

    private static final List<String> ENVIRONMENTS = List.of("local", "hml", "prd");

    private static final List<String> OPERATIONS = List.of(
            "create-order",
            "calculate-freight",
            "search-product",
            "process-payment",
            "refresh-resources"
    );

    // mapeamento determinístico service -> error_code principal
    // cobre: fluxo encadeado elastic_field_capabilities -> elastic_search_logs com filtro combinado
    private static final Map<String, String> SERVICE_ERROR_CODE = Map.of(
            "payment-service",  "PAYMENT_TIMEOUT",
            "delivery-service", "DELIVERY_UNAVAILABLE",
            "checkout-service", "DOWNSTREAM_4XX",
            "catalog-service",  "DOWNSTREAM_5XX",
            "mcp-server",       "UNEXPECTED_ERROR"
    );

    private static final List<String> ALL_ERROR_CODES = List.of(
            "PAYMENT_TIMEOUT",
            "DELIVERY_UNAVAILABLE",
            "DOWNSTREAM_4XX",
            "DOWNSTREAM_5XX",
            "UNEXPECTED_ERROR"
    );

    // pool reduzido de CPFs — permite repetição entre logs distintos
    private static final List<String> CPF_POOL = List.of(
            "123.456.789-00",
            "987.654.321-11",
            "111.222.333-44",
            "555.666.777-88",
            "000.111.222-33",
            "444.555.666-77",
            "321.654.987-99",
            "741.852.963-55"
    );

    // pool reduzido de números de pedido — permitze repetição
    private static final List<String> ORDER_NUMBER_POOL = List.of(
            "PED-100001", "PED-100002", "PED-100003", "PED-100004",
            "PED-100005", "PED-100006", "PED-100007", "PED-100008",
            "PED-100009", "PED-100010", "PED-100011", "PED-100012"
    );

    // pool reduzido de números de entrega — permite repetição
    private static final List<String> DELIVERY_NUMBER_POOL = List.of(
            "ENT-200001", "ENT-200002", "ENT-200003", "ENT-200004",
            "ENT-200005", "ENT-200006", "ENT-200007", "ENT-200008",
            "ENT-200009", "ENT-200010"
    );

    private static final List<String> PAYMENT_TYPES = List.of(
            "CREDIT_CARD",
            "DEBIT_CARD",
            "PIX",
            "BOLETO",
            "VOUCHER",
            "WALLET"
    );

    // número de batches — cada batch dorme um intervalo para distribuir timestamps
    // cobre: elastic_aggregate_logs date_histogram (tendência temporal)
    private static final int BATCHES = 4;

    // delay entre batches em ms — suficiente para gerar timestamps distintos no Elasticsearch
    private static final int BATCH_DELAY_MS = 1_500;

    @Value("${stub.logs.total:300}")
    private int totalLogs;

    @Override
    public void run(ApplicationArguments args) {
        final int amount = Math.max(BATCHES * 10, totalLogs);
        final int perBatch = amount / BATCHES;

        log.info("Iniciando geração de logs stub. totalLogs={}, batches={}, perBatch={}", amount, BATCHES, perBatch);

        for (int batch = 1; batch <= BATCHES; batch++) {
            final String batchId = UUID.randomUUID().toString().replace("-", "");
            log.info("Iniciando batch stub. batch={}/{}, batchId={}", batch, BATCHES, batchId);

            // logs aleatórios — cobrem o volume geral de todos os cenários
            for (int seq = 1; seq <= perBatch; seq++) {
                emitRandomLog(batchId, seq);
            }

            // logs determinísticos por serviço — garantem volume mínimo para:
            //   elastic_count_logs (quantos erros por serviço)
            //   elastic_aggregate_logs terms (top serviços com mais erros)
            //   elastic_multi_search_logs (comparação payment-service vs checkout-service)
            //   elastic_terms_enum prefix "pay" (payment-service sempre presente)
            for (String service : SERVICES) {
                emitServiceGuaranteedLogs(batchId, service);
            }

            // logs com latência alta — garantem resultados para:
            //   elastic_search_logs query=latency_ms:[2000 TO *]
            emitHighLatencyLogs(batchId);

            // logs com http_status=500 garantidos por serviço — cobrem:
            //   elastic_search_logs query=http_status:500
            //   elastic_count_logs query=http_status:500
            emitGuaranteedHttpErrorLogs(batchId);

            // logs de pedido com CPF, número de pedido, número de entrega e tipo de pagamento
            // pool reduzido para garantir repetição de valores entre logs distintos
            emitOrderLogs(batchId);

            if (batch < BATCHES) {
                sleepQuietly(BATCH_DELAY_MS);
            }
        }

        log.info("Geração de logs stub concluída. totalLogs={}, batches={}", amount, BATCHES);
    }

    // -------------------------------------------------------------------------
    // logs aleatórios gerais
    // -------------------------------------------------------------------------

    private void emitRandomLog(String batchId, int sequence) {
        final var random = ThreadLocalRandom.current();
        final String service = randomItem(SERVICES);
        emitLog(
                batchId,
                sequence,
                service,
                randomItem(ENVIRONMENTS),
                randomItem(OPERATIONS),
                random.nextInt(25, 3000),
                randomHttpStatus(),
                "user-" + random.nextInt(1, 250),
                randomItem(ALL_ERROR_CODES),
                chooseLevel(sequence)
        );
    }

    // -------------------------------------------------------------------------
    // logs determinísticos: volume mínimo garantido por serviço
    // cobre elastic_count_logs, elastic_aggregate_logs terms, elastic_multi_search_logs
    // -------------------------------------------------------------------------

    private void emitServiceGuaranteedLogs(String batchId, String service) {
        final var random = ThreadLocalRandom.current();
        final String errorCode = SERVICE_ERROR_CODE.get(service);

        // 3 INFO + 3 WARN + 4 ERROR por serviço por batch = volume consistente para agregações
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(50, 500),   200, "user-1",  errorCode, "INFO");
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(50, 500),   200, "user-2",  errorCode, "INFO");
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(50, 500),   201, "user-3",  errorCode, "INFO");
        emitLog(batchId, 0, service, "hml", randomItem(OPERATIONS), random.nextInt(500, 1500), 400, "user-10", errorCode, "WARN");
        emitLog(batchId, 0, service, "hml", randomItem(OPERATIONS), random.nextInt(500, 1500), 404, "user-11", errorCode, "WARN");
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(500, 1500), 400, "user-12", errorCode, "WARN");
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(800, 2500), 500, "user-20", errorCode, "ERROR");
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(800, 2500), 500, "user-21", errorCode, "ERROR");
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(800, 2500), 500, "user-22", errorCode, "ERROR");
        emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS), random.nextInt(800, 2500), 500, "user-23", errorCode, "ERROR");
    }

    // -------------------------------------------------------------------------
    // logs com latência alta (>2000ms)
    // cobre elastic_search_logs query=latency_ms:[2000 TO *]
    // -------------------------------------------------------------------------

    private void emitHighLatencyLogs(String batchId) {
        final var random = ThreadLocalRandom.current();
        for (String service : SERVICES) {
            final int latency = random.nextInt(2000, 3000);
            emitLog(batchId, 0, service, "prd", "process-payment", latency, 500,
                    "user-" + random.nextInt(1, 250), SERVICE_ERROR_CODE.get(service), "ERROR");
            emitLog(batchId, 0, service, "prd", "create-order", latency, 200,
                    "user-" + random.nextInt(1, 250), SERVICE_ERROR_CODE.get(service), "WARN");
        }
    }

    // -------------------------------------------------------------------------
    // logs http_status=500 garantidos — cobre elastic_search_logs e elastic_count_logs
    // -------------------------------------------------------------------------

    private void emitGuaranteedHttpErrorLogs(String batchId) {
        final var random = ThreadLocalRandom.current();
        // 2 logs status=500 por serviço por batch — volume suficiente para buscas e contagens
        for (String service : SERVICES) {
            emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS),
                    random.nextInt(800, 3000), 500,
                    "user-" + random.nextInt(1, 250),
                    SERVICE_ERROR_CODE.get(service), "ERROR");
            emitLog(batchId, 0, service, "prd", randomItem(OPERATIONS),
                    random.nextInt(800, 3000), 500,
                    "user-" + random.nextInt(1, 250),
                    SERVICE_ERROR_CODE.get(service), "ERROR");
        }
    }

    // -------------------------------------------------------------------------
    // logs de pedido: CPF, número de pedido, número de entrega, tipo de pagamento
    // pools reduzidos para garantir repetição de valores entre logs distintos
    // -------------------------------------------------------------------------

    private void emitOrderLogs(String batchId) {
        final var random = ThreadLocalRandom.current();
        // 20 logs por batch — mistura de serviços, status e níveis
        // ~30% dos logs reutilizam CPF/pedido/entrega do pool (repetição intencional)
        for (int i = 0; i < 20; i++) {
            final String service      = randomItem(SERVICES);
            final String operation    = randomItem(List.of("create-order", "process-payment", "calculate-freight"));
            final int    httpStatus   = randomHttpStatus();
            final String level        = httpStatus >= 500 ? "ERROR" : httpStatus >= 400 ? "WARN" : "INFO";
            final String errorCode    = SERVICE_ERROR_CODE.get(service);

            // a cada 3 logs força reuso de índices baixos do pool para criar repetição
            final boolean reuse       = (i % 3 == 0);
            final String  cpf         = reuse ? CPF_POOL.get(random.nextInt(3))
                                              : randomItem(CPF_POOL);
            final String  orderNumber = reuse ? ORDER_NUMBER_POOL.get(random.nextInt(4))
                                              : randomItem(ORDER_NUMBER_POOL);
            final String  deliveryNum = reuse ? DELIVERY_NUMBER_POOL.get(random.nextInt(3))
                                              : randomItem(DELIVERY_NUMBER_POOL);
            final String  paymentType = randomItem(PAYMENT_TYPES);

            emitOrderLog(batchId, i, service, randomItem(ENVIRONMENTS), operation,
                    random.nextInt(50, 3000), httpStatus,
                    "user-" + random.nextInt(1, 250),
                    errorCode, level,
                    cpf, orderNumber, deliveryNum, paymentType);
        }
    }

    private void emitOrderLog(String batchId, int sequence, String service, String environment,
                              String operation, int latencyMs, int httpStatus,
                              String userId, String errorCode, String level,
                              String cpf, String orderNumber, String deliveryNumber, String paymentType) {
        final String correlationId = UUID.randomUUID().toString();
        final String traceId       = UUID.randomUUID().toString().replace("-", "");

        final Map<String, String> mdc = new HashMap<>();
        mdc.put("stub",            "true");
        mdc.put("stub_batch_id",   batchId);
        mdc.put("stub_sequence",   String.valueOf(sequence));
        mdc.put("service_name",    service);
        mdc.put("environment",     environment);
        mdc.put("operation",       operation);
        mdc.put("http_status",     String.valueOf(httpStatus));
        mdc.put("latency_ms",      String.valueOf(latencyMs));
        mdc.put("correlation_id",  correlationId);
        mdc.put("trace_id",        traceId);
        mdc.put("user_id",         userId);
        mdc.put("error_code",      errorCode);
        mdc.put("cpf",             cpf);
        mdc.put("order_number",    orderNumber);
        mdc.put("delivery_number", deliveryNumber);
        mdc.put("payment_type",    paymentType);

        final String message = "stub-log event=order service={} environment={} operation={} status={} " +
                "latencyMs={} correlationId={} traceId={} userId={} errorCode={} " +
                "cpf={} orderNumber={} deliveryNumber={} paymentType={} batchId={} sequence={}";

        withMdc(mdc, () -> {
            switch (level) {
                case "INFO" -> log.info(message, service, environment, operation, httpStatus,
                        latencyMs, correlationId, traceId, userId, errorCode,
                        cpf, orderNumber, deliveryNumber, paymentType, batchId, sequence);
                case "WARN" -> log.warn(message, service, environment, operation, httpStatus,
                        latencyMs, correlationId, traceId, userId, errorCode,
                        cpf, orderNumber, deliveryNumber, paymentType, batchId, sequence);
                default     -> log.error(message, service, environment, operation, httpStatus,
                        latencyMs, correlationId, traceId, userId, errorCode,
                        cpf, orderNumber, deliveryNumber, paymentType, batchId, sequence);
            }
        });
    }

    // -------------------------------------------------------------------------
    // emissão centralizada
    // -------------------------------------------------------------------------

    private void emitLog(String batchId, int sequence, String service, String environment,
                         String operation, int latencyMs, int httpStatus,
                         String userId, String errorCode, String level) {
        final String correlationId = UUID.randomUUID().toString();
        final String traceId = UUID.randomUUID().toString().replace("-", "");

        final Map<String, String> mdc = new HashMap<>();
        mdc.put("stub", "true");
        mdc.put("stub_batch_id", batchId);
        mdc.put("stub_sequence", String.valueOf(sequence));
        mdc.put("service_name", service);
        mdc.put("environment", environment);
        mdc.put("operation", operation);
        mdc.put("http_status", String.valueOf(httpStatus));
        mdc.put("latency_ms", String.valueOf(latencyMs));
        mdc.put("correlation_id", correlationId);
        mdc.put("trace_id", traceId);
        mdc.put("user_id", userId);
        mdc.put("error_code", errorCode);

        final String message = "stub-log event=transaction service={} environment={} operation={} status={} " +
                "latencyMs={} correlationId={} traceId={} userId={} errorCode={} batchId={} sequence={}";

        withMdc(mdc, () -> {
            switch (level) {
                case "INFO" -> log.info(message, service, environment, operation, httpStatus,
                        latencyMs, correlationId, traceId, userId, errorCode, batchId, sequence);
                case "WARN" -> log.warn(message, service, environment, operation, httpStatus,
                        latencyMs, correlationId, traceId, userId, errorCode, batchId, sequence);
                default   -> log.error(message, service, environment, operation, httpStatus,
                        latencyMs, correlationId, traceId, userId, errorCode, batchId, sequence);
            }
        });
    }

    // -------------------------------------------------------------------------
    // utilitários
    // -------------------------------------------------------------------------

    private int randomHttpStatus() {
        final int draw = ThreadLocalRandom.current().nextInt(100);
        if (draw < 68) return 200;
        if (draw < 80) return 201;
        if (draw < 88) return 400;
        if (draw < 95) return 404;
        return 500;
    }

    private String chooseLevel(int sequence) {
        if (sequence == 1) return "INFO";
        if (sequence == 2) return "WARN";
        if (sequence == 3) return "ERROR";
        final int draw = ThreadLocalRandom.current().nextInt(100);
        if (draw < 60) return "INFO";
        if (draw < 85) return "WARN";
        return "ERROR";
    }

    private String randomItem(List<String> source) {
        return source.get(ThreadLocalRandom.current().nextInt(source.size()));
    }

    private void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void withMdc(Map<String, String> mdcEntries, Runnable action) {
        final Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            mdcEntries.forEach(MDC::put);
            action.run();
        } finally {
            MDC.clear();
            if (previousContext != null && !previousContext.isEmpty()) {
                MDC.setContextMap(previousContext);
            }
        }
    }
}
