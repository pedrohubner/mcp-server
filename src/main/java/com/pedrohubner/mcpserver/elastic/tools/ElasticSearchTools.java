package com.pedrohubner.mcpserver.elastic.tools;

import com.pedrohubner.mcpserver.elastic.ElasticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchTools {
    private final ElasticSearchService elasticSearchService;

    @McpTool(
            name = "elastic_search_logs",
            description = "Busca documentos de log no Elasticsearch usando filtros textuais e intervalo de tempo. " +
                    "USE quando o usuário precisar ver o conteúdo completo dos eventos (mensagem, stacktrace, campos contextuais), ordenados do mais recente para o mais antigo. " +
                    "NÃO USE para contar ocorrências (prefira elastic_count_logs), para tendências no tempo (prefira elastic_aggregate_logs) ou para buscas em janelas muito longas/alto volume (prefira elastic_async_search_submit). " +
                    "RETORNA: lista de hits com todos os campos do documento (_source), total de documentos encontrados e metadados da query. " +
                    "DICA: se não souber o indexPattern correto, chame elastic_list_indices antes. Se não souber os campos disponíveis, chame elastic_field_capabilities antes. " +
                    "Exemplos de uso: " +
                    "(1) 'Mostre os últimos 50 erros do serviço de pagamento hoje' -> indexPattern=mcp-server-logs-*, query=level:ERROR AND service:pagamento, size=50, fromTimestamp=inicio-do-dia-atual, toTimestamp=agora. " +
                    "(2) 'Busque logs com NullPointerException na última hora' -> query=message:NullPointerException, fromTimestamp=uma-hora-atras, toTimestamp=agora. " +
                    "(3) 'Quais os últimos 10 warnings do módulo checkout?' -> query=level:WARN AND module:checkout, size=10."
    )
    public Map<String, Object> elasticSearchLogs(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream. Se omitido, usa o padrão default da aplicação. Use elastic_list_indices para descobrir padrões disponíveis.")
            String indexPattern,
            @McpArg(name = "query", description = "Consulta no formato query_string do Elasticsearch. Suporta operadores AND, OR, NOT e wildcards. Exemplos: level:ERROR AND service:auth, message:\"connection refused\", level:(WARN OR ERROR).")
            String query,
            @McpArg(name = "size", description = "Quantidade máxima de documentos retornados (1-200). Use valores menores (10-20) para inspeção rápida e maiores (50-200) para análise de padrões.")
            Integer size,
            @McpArg(name = "fromTimestamp", description = "Limite inferior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor para obter a data/hora UTC exata do sistema. Nunca assuma o horário atual. Exemplos reais: '2026-02-23T14:00:00Z' (início de uma hora específica), '2026-02-23T13:55:00Z' (5 minutos atrás, dado que agora é 14:00:00Z).")
            String fromTimestamp,
            @McpArg(name = "toTimestamp", description = "Limite superior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor para obter a data/hora UTC exata do sistema. Nunca assuma o horário atual. Exemplo real: '2026-02-23T14:00:00Z' para o momento atual obtido via terminal.")
            String toTimestamp) {
        try {
            return elasticSearchService.searchLogs(indexPattern, query, size, fromTimestamp, toTimestamp);
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_search_logs", ex);
            return this.errorResponse("elastic_search_logs", ex);
        }
    }

    @McpTool(
            name = "elastic_count_logs",
            description = "Conta quantos documentos de log satisfazem os filtros informados, sem retornar os eventos completos. " +
                    "USE quando o usuário quiser apenas um número (quantidade de ocorrências), sem necessidade de ver o conteúdo dos logs. " +
                    "NÃO USE se o usuário precisar ver os detalhes dos eventos (prefira elastic_search_logs) ou analisar tendência ao longo do tempo (prefira elastic_aggregate_logs com date_histogram). " +
                    "RETORNA: um único campo 'count' com o total de documentos que satisfazem os filtros. " +
                    "DICA: se não souber o indexPattern correto, chame elastic_list_indices antes. " +
                    "Exemplos de uso: " +
                    "(1) 'Quantos erros ocorreram hoje no serviço de autenticação?' -> query=level:ERROR AND service:auth, fromTimestamp=inicio-do-dia-atual, toTimestamp=agora. " +
                    "(2) 'Quantos warnings foram gerados na última semana?' -> query=level:WARN, fromTimestamp=sete-dias-atras, toTimestamp=agora. " +
                    "(3) 'Quantos logs com timeout existem no índice de pedidos?' -> indexPattern=orders-logs-*, query=message:timeout."
    )
    public Map<String, Object> elasticCountLogs(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream. Se omitido, usa o padrão default da aplicação. Use elastic_list_indices para descobrir padrões disponíveis.")
            String indexPattern,
            @McpArg(name = "query", description = "Consulta no formato query_string do Elasticsearch. Suporta operadores AND, OR, NOT e wildcards. Exemplos: level:ERROR, level:WARN AND service:checkout, message:timeout.")
            String query,
            @McpArg(name = "fromTimestamp", description = "Limite inferior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual. Exemplos reais: '2026-02-23T00:00:00Z' (início do dia), '2026-02-16T14:00:00Z' (sete dias atrás, dado que agora é 2026-02-23T14:00:00Z).")
            String fromTimestamp,
            @McpArg(name = "toTimestamp", description = "Limite superior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String toTimestamp) {
        try {
            return elasticSearchService.countLogs(indexPattern, query, fromTimestamp, toTimestamp);
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_count_logs", ex);
            return this.errorResponse("elastic_count_logs", ex);
        }
    }

    @McpTool(
            name = "elastic_list_indices",
            description = "Lista índices no Elasticsearch a partir de um padrão informado. " +
                    "USE como primeiro passo de descoberta quando não souber o nome exato do índice ou quiser confirmar quais índices existem antes de executar outras tools. " +
                    "NÃO USE para buscar documentos (prefira elastic_search_logs) ou inspecionar campos (prefira elastic_field_capabilities). " +
                    "RETORNA: lista de índices com nome, status, número de documentos, tamanho em disco e saúde. " +
                    "DICA: chame esta tool antes de elastic_search_logs, elastic_count_logs ou elastic_aggregate_logs sempre que o indexPattern for desconhecido. " +
                    "Exemplos de uso: " +
                    "(1) 'Quais índices existem para logs do mcp-server?' -> indexPattern=mcp-server-logs-*. " +
                    "(2) 'Liste todos os índices disponíveis no cluster' -> indexPattern=*. " +
                    "(3) 'Existem índices de logs deste mês?' -> indexPattern=*-2026.02.*."
    )
    public Map<String, Object> elasticListIndices(
            @McpArg(name = "indexPattern", description = "Padrão de índice. Exemplo: mcp-server-logs-*.")
            String indexPattern
    ) {
        try {
            return elasticSearchService.listIndices(indexPattern);
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_list_indices", ex);
            return this.errorResponse("elastic_list_indices", ex);
        }
    }

    @McpTool(
            name = "elastic_field_capabilities",
            description = "Descobre o schema efetivo dos campos em um conjunto de índices. " +
                    "USE antes de construir queries de agregação ou filtros por campo quando não souber o tipo ou nome exato dos campos disponíveis. " +
                    "NÃO USE para listar índices (prefira elastic_list_indices) ou buscar valores distintos de um campo (prefira elastic_terms_enum). " +
                    "RETORNA: mapa de campo -> tipo (keyword, text, date, long etc.) e metadados de pesquisabilidade e agregabilidade por índice. " +
                    "DICA: chame esta tool antes de elastic_aggregate_logs (para confirmar o campo correto) e antes de elastic_search_logs (para montar o filtro com o nome de campo correto). " +
                    "Exemplos de uso: " +
                    "(1) 'Quais campos estão disponíveis nos logs do mcp-server?' -> indexPattern=mcp-server-logs-*, fields=*. " +
                    "(2) 'Qual o tipo do campo level nos índices de log?' -> indexPattern=mcp-server-logs-*, fields=level. " +
                    "(3) 'Mostre os campos @timestamp, level e message, incluindo não mapeados' -> fields=@timestamp,level,message, includeUnmapped=true."
    )
    public Map<String, Object> elasticFieldCapabilities(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream alvo da inspeção de schema.")
            String indexPattern,
            @McpArg(name = "fields", description = "Lista de campos separada por vírgula. Exemplo: @timestamp,level,message,* (default '*').")
            String fields,
            @McpArg(name = "includeUnmapped", description = "Quando true, inclui também campos não mapeados para parte dos índices.")
            Boolean includeUnmapped
    ) {
        try {
            return elasticSearchService.fieldCapabilities(indexPattern, fields, includeUnmapped);
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_field_capabilities", ex);
            return this.errorResponse("elastic_field_capabilities", ex);
        }
    }

    @McpTool(
            name = "elastic_terms_enum",
            description = "Enumera valores distintos de um campo para exploração guiada por linguagem natural e descoberta de valores existentes (ex.: quais serviços, ambientes ou status existem). " +
                    "USE quando precisar descobrir os valores possíveis de um campo antes de filtrar, ou para listar opções disponíveis. " +
                    "NÃO USE para buscar documentos completos (prefira elastic_search_logs) ou para contar ocorrências por valor (prefira elastic_aggregate_logs com terms). " +
                    "RETORNA: lista de termos (strings) encontrados no campo informado, respeitando prefixo e filtros opcionais. " +
                    "DICA: use elastic_field_capabilities antes para confirmar o nome e tipo do campo (.keyword é obrigatório para campos de texto). " +
                    "Exemplos de uso: " +
                    "(1) 'Quais serviços existem nos logs?' -> indexPattern=mcp-server-logs-*, field=service.keyword, size=50. " +
                    "(2) 'Liste os serviços que começam com \"pagamento\"' -> field=service.keyword, prefix=pagamento. " +
                    "(3) 'Quais ambientes aparecem nos logs de erro da última semana?' -> field=environment.keyword, query=level:ERROR, fromTimestamp=sete-dias-atras, toTimestamp=agora."
    )
    public Map<String, Object> elasticTermsEnum(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream. Se omitido, usa o padrão default da aplicação. Use elastic_list_indices para descobrir padrões disponíveis.")
            String indexPattern,
            @McpArg(name = "field", description = "Campo alvo para enumeração de termos. Deve ser do tipo keyword para campos de texto — use o sufixo .keyword (ex.: service.keyword, environment.keyword, level.keyword). Use elastic_field_capabilities para confirmar o nome exato.")
            String field,
            @McpArg(name = "prefix", description = "Prefixo opcional para filtrar os termos retornados. Útil para autocomplete: ex.: 'pag' retorna 'pagamento', 'pagamentos-api'. Se omitido, retorna todos os termos do campo.")
            String prefix,
            @McpArg(name = "size", description = "Quantidade máxima de termos retornados (1-1000). Use valores menores (10-50) para exploração rápida e maiores para listagem completa.")
            Integer size,
            @McpArg(name = "query", description = "Filtro textual opcional no formato query_string para restringir o universo de documentos analisados antes de enumerar os termos. Exemplos: level:ERROR, service:auth.")
            String query,
            @McpArg(name = "fromTimestamp", description = "Limite inferior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ) para restringir o universo de documentos analisados. OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String fromTimestamp,
            @McpArg(name = "toTimestamp", description = "Limite superior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String toTimestamp,
            @McpArg(name = "caseInsensitive", description = "Quando true, ignora diferença entre maiúsculas e minúsculas na busca pelo prefixo. Útil quando os valores do campo podem ter capitalização inconsistente.")
            Boolean caseInsensitive
    ) {
        try {
            return elasticSearchService.termsEnum(
                    indexPattern, field, prefix, size, query, fromTimestamp, toTimestamp, caseInsensitive
            );
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_terms_enum", ex);
            return this.errorResponse("elastic_terms_enum", ex);
        }
    }

    @McpTool(
            name = "elastic_aggregate_logs",
            description = "Executa agregações analíticas em logs sem retornar hits brutos. " +
                    "Suporta date_histogram (tendência no tempo), terms (ranking de valores) e cardinality (contagem de valores únicos). " +
                    "USE para perguntas analíticas como 'quantos erros por hora', 'top serviços com mais falhas' ou 'quantos trace IDs únicos'. " +
                    "NÃO USE para ver o conteúdo dos eventos (prefira elastic_search_logs). Para janelas muito longas ou agregações muito custosas, prefira elastic_async_search_submit. " +
                    "RETORNA: buckets de agregação com chave e contagem (date_histogram/terms) ou valor numérico único (cardinality). " +
                    "DICA: use elastic_field_capabilities antes para confirmar o nome exato do campo de agregação (.keyword para terms/cardinality em campos de texto). " +
                    "Exemplos de uso: " +
                    "(1) 'Quantos erros por hora ocorreram hoje?' -> aggregationType=date_histogram, aggregationField=@timestamp, interval=1h, query=level:ERROR, fromTimestamp=inicio-do-dia-atual, toTimestamp=agora. " +
                    "(2) 'Quais os 10 serviços com mais erros na última semana?' -> aggregationType=terms, aggregationField=service.keyword, topN=10, query=level:ERROR, fromTimestamp=sete-dias-atras, toTimestamp=agora. " +
                    "(3) 'Quantos trace IDs únicos existem nos logs de hoje?' -> aggregationType=cardinality, aggregationField=traceId.keyword, fromTimestamp=inicio-do-dia-atual, toTimestamp=agora."
    )
    public Map<String, Object> elasticAggregateLogs(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream. Se omitido, usa o padrão default da aplicação. Use elastic_list_indices para descobrir padrões disponíveis.")
            String indexPattern,
            @McpArg(name = "query", description = "Filtro textual no formato query_string aplicado antes da agregação. Exemplos: level:ERROR, service:auth AND level:WARN. Se omitido, agrega sobre todos os documentos.")
            String query,
            @McpArg(name = "fromTimestamp", description = "Limite inferior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual. Exemplos reais: início do dia = '2026-02-23T00:00:00Z', sete dias atrás = '2026-02-16T14:00:00Z' (dado que agora é '2026-02-23T14:00:00Z').")
            String fromTimestamp,
            @McpArg(name = "toTimestamp", description = "Limite superior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String toTimestamp,
            @McpArg(name = "aggregationType", description = "Tipo da agregação: 'date_histogram' para tendência ao longo do tempo, 'terms' para ranking dos valores mais frequentes de um campo, 'cardinality' para contar valores únicos de um campo.")
            String aggregationType,
            @McpArg(name = "aggregationField", description = "Campo alvo da agregação. Para date_histogram, use @timestamp (ou omita para usar o default). Para terms e cardinality em campos de texto, use o sufixo .keyword (ex.: service.keyword, level.keyword). Use elastic_field_capabilities para confirmar o nome exato.")
            String aggregationField,
            @McpArg(name = "interval", description = "Intervalo temporal para date_histogram. Exemplos: 1m (por minuto), 5m, 15m, 1h (por hora), 1d (por dia). Ignorado para aggregationType terms e cardinality.")
            String interval,
            @McpArg(name = "topN", description = "Quantidade de valores no ranking para aggregationType terms (default 10). Ex.: 5 retorna os 5 campos mais frequentes. Ignorado para date_histogram e cardinality.")
            Integer topN
    ) {
        try {
            return elasticSearchService.aggregateLogs(
                    indexPattern, query, fromTimestamp, toTimestamp, aggregationType, aggregationField, interval, topN
            );
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_aggregate_logs", ex);
            return this.errorResponse("elastic_aggregate_logs", ex);
        }
    }

    @McpTool(
            name = "elastic_multi_search_logs",
            description = "Executa múltiplas consultas em uma única chamada (_msearch), retornando resultados lado a lado. " +
                    "USE quando o usuário quiser comparar dois ou mais filtros simultaneamente (ex.: erros vs warnings, serviço A vs serviço B). " +
                    "NÃO USE para uma única consulta (prefira elastic_search_logs) ou para agregações (prefira elastic_aggregate_logs). Para volumes altos em múltiplas queries, prefira elastic_async_search_submit por query. " +
                    "RETORNA: array de respostas, uma por query informada, cada uma com seus próprios hits e metadados. " +
                    "DICA: as queries compartilham o mesmo indexPattern, fromTimestamp e toTimestamp — use quando os filtros diferirem apenas na query em si. " +
                    "Exemplos de uso: " +
                    "(1) 'Compare erros e warnings de hoje em paralelo' -> queries=[\"level:ERROR\", \"level:WARN\"], fromTimestamp=inicio-do-dia-atual, toTimestamp=agora. " +
                    "(2) 'Busque logs de timeout e de conexão recusada ao mesmo tempo' -> queries=[\"message:timeout\", \"message:\\\"connection refused\\\"\"], size=20. " +
                    "(3) 'Traga os últimos logs dos serviços auth e checkout simultaneamente' -> queries=[\"service:auth\", \"service:checkout\"], size=50."
    )
    public Map<String, Object> elasticMultiSearchLogs(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream aplicado a todas as queries. Use elastic_list_indices para descobrir padrões disponíveis.")
            String indexPattern,
            @McpArg(name = "queries", description = "Lista de queries no formato query_string, uma por comparação desejada. Cada query gera uma resposta independente no resultado. Exemplos: [\"level:ERROR\", \"level:WARN\"], [\"service:auth\", \"service:checkout\"]. Mínimo 2 queries para fazer sentido usar esta tool.")
            List<String> queries,
            @McpArg(name = "size", description = "Quantidade de hits retornados por query (1-200). O mesmo limite é aplicado a todas as queries da lista.")
            Integer size,
            @McpArg(name = "fromTimestamp", description = "Limite inferior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ) aplicado a todas as queries. OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual. Exemplos reais: início do dia = '2026-02-23T00:00:00Z', uma hora atrás = '2026-02-23T13:00:00Z' (dado que agora é '2026-02-23T14:00:00Z').")
            String fromTimestamp,
            @McpArg(name = "toTimestamp", description = "Limite superior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ) aplicado a todas as queries. OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String toTimestamp
    ) {
        try {
            return elasticSearchService.multiSearch(indexPattern, queries, size, fromTimestamp, toTimestamp);
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_multi_search_logs", ex);
            return this.errorResponse("elastic_multi_search_logs", ex);
        }
    }

    @McpTool(
            name = "elastic_async_search_submit",
            description = "Submete uma consulta assíncrona para cenários pesados: janelas de tempo longas (semanas/meses), alto volume de documentos ou agregações custosas. " +
                    "USE quando elastic_search_logs ou elastic_aggregate_logs puderem ser lentos ou estourar timeout. " +
                    "NÃO USE para buscas simples e rápidas (prefira elastic_search_logs ou elastic_aggregate_logs). " +
                    "RETORNA: um 'asyncSearchId' para acompanhar o progresso, status de conclusão (isRunning) e resultados parciais se disponíveis dentro do waitForCompletionTimeout. " +
                    "ATENÇÃO: após receber o asyncSearchId, informe ao usuário que a busca está em andamento e que o ID pode ser usado para verificar o resultado posteriormente. " +
                    "Exemplos de uso: " +
                    "(1) 'Busque erros do último mês sem travar' -> query=level:ERROR, fromTimestamp=um-mes-atras, toTimestamp=agora, waitForCompletionTimeout=1s, keepAlive=10m. " +
                    "(2) 'Submeta agregação pesada nos últimos 90 dias' -> query=level:ERROR, fromTimestamp=noventa-dias-atras, size=200, waitForCompletionTimeout=500ms, keepAlive=30m. " +
                    "(3) 'Inicie busca grande no índice histórico mantendo resultado por 1 hora' -> indexPattern=mcp-server-logs-2025-*, query=message:exception, keepAlive=1h."
    )
    public Map<String, Object> elasticAsyncSearchSubmit(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream. Use elastic_list_indices para descobrir padrões disponíveis.")
            String indexPattern,
            @McpArg(name = "query", description = "Consulta no formato query_string do Elasticsearch. Suporta operadores AND, OR, NOT e wildcards. Exemplos: level:ERROR AND service:auth, message:exception, level:(WARN OR ERROR).")
            String query,
            @McpArg(name = "size", description = "Quantidade máxima de hits desejada na resposta assíncrona (1-200). Prefer valores menores para reduzir carga no cluster em janelas longas.")
            Integer size,
            @McpArg(name = "fromTimestamp", description = "Limite inferior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual. Para janelas longas, use referências como 'trinta dias atrás = data atual menos 30 dias', sempre com hora, minuto e segundo explícitos.")
            String fromTimestamp,
            @McpArg(name = "toTimestamp", description = "Limite superior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String toTimestamp,
            @McpArg(name = "waitForCompletionTimeout", description = "Tempo máximo de espera síncrona antes de retornar o controle com o asyncSearchId. Se a busca completar antes, retorna resultado imediato. Exemplos: 500ms (retorno rápido), 1s, 5s (espera um pouco mais). Para buscas sabidamente pesadas, use 500ms.")
            String waitForCompletionTimeout,
            @McpArg(name = "keepAlive", description = "Tempo de retenção dos resultados no cluster após a conclusão da busca assíncrona. Após este prazo, o resultado é descartado. Exemplos: 1m (curto), 10m (médio), 1h (longo para resultado consultado sob demanda).")
            String keepAlive
    ) {
        try {
            return elasticSearchService.submitAsyncSearch(
                    indexPattern, query, size, fromTimestamp, toTimestamp, waitForCompletionTimeout, keepAlive
            );
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_async_search_submit", ex);
            return this.errorResponse("elastic_async_search_submit", ex);
        }
    }

    @McpTool(
            name = "elastic_validate_query",
            description = "Valida a sintaxe e estrutura de uma query antes de executá-la, funcionando como guardrail para queries geradas por IA. " +
                    "USE antes de elastic_search_logs ou elastic_aggregate_logs sempre que a query for complexa ou gerada automaticamente, para evitar erros em tempo de execução. " +
                    "NÃO USE para executar a busca de fato (esta tool não retorna documentos). " +
                    "RETORNA: campo 'valid' (boolean) indicando se a query é válida, e 'explanations' com detalhes por shard quando explain=true. " +
                    "DICA: use explain=true para obter o motivo exato da falha quando a query for inválida. " +
                    "Exemplos de uso: " +
                    "(1) 'Verifique se a query level:ERROR AND service:auth é válida antes de executar' -> query=level:ERROR AND service:auth, explain=true. " +
                    "(2) 'Valide a query com intervalo de tempo no índice de logs' -> indexPattern=mcp-server-logs-*, query=message:timeout, fromTimestamp=inicio-do-dia-atual. " +
                    "(3) 'Cheque a query em todos os shards antes de rodar' -> query=level:WARN AND module:checkout, allShards=true, explain=true."
    )
    public Map<String, Object> elasticValidateQuery(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream onde a query será validada. Quanto mais específico, mais precisa a validação.")
            String indexPattern,
            @McpArg(name = "query", description = "Consulta no formato query_string a ser validada. Exemplos: level:ERROR AND service:auth, message:\"connection refused\", level:(WARN OR ERROR).")
            String query,
            @McpArg(name = "fromTimestamp", description = "Limite inferior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ) incluído na validação para simular a query completa que seria executada. OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String fromTimestamp,
            @McpArg(name = "toTimestamp", description = "Limite superior de tempo em formato ISO-8601 completo (yyyy-MM-dd'T'HH:mm:ssZ). OBRIGATÓRIO: execute o comando 'date -u +%Y-%m-%dT%H:%M:%SZ' no terminal ANTES de calcular este valor. Nunca assuma o horário atual.")
            String toTimestamp,
            @McpArg(name = "explain", description = "Quando true, retorna explicação detalhada por cláusula da query — essencial para identificar exatamente qual parte da query é inválida. Recomendado sempre que a validação retornar false.")
            Boolean explain,
            @McpArg(name = "allShards", description = "Quando true, executa a validação em todos os shards do índice em vez de apenas um. Aumenta a precisão em índices com mapeamentos divergentes entre shards, mas tem custo maior.")
            Boolean allShards
    ) {
        try {
            return elasticSearchService.validateQuery(
                    indexPattern, query, fromTimestamp, toTimestamp, explain, allShards
            );
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_validate_query", ex);
            return this.errorResponse("elastic_validate_query", ex);
        }
    }

    @McpTool(
            name = "elastic_cluster_health",
            description = "Consulta a saúde do cluster Elasticsearch e o estado de alocação de shards. " +
                    "USE para diagnóstico operacional quando suspeitar de problemas no cluster, índices em estado degradado ou shards não alocados. " +
                    "NÃO USE para buscar documentos ou estatísticas de volume (prefira elastic_index_stats para métricas de docs/store). " +
                    "RETORNA: status geral do cluster (green/yellow/red), número de nós, shards ativos/relocando/não alocados e, quando level=indices ou shards, detalhes por índice ou shard. " +
                    "Exemplos de uso: " +
                    "(1) 'Como está a saúde geral do cluster?' -> level=cluster. " +
                    "(2) 'Verifique a saúde dos índices de logs do mcp-server' -> indexPattern=mcp-server-logs-*, level=indices. " +
                    "(3) 'Aguarde até o cluster ficar verde e mostre detalhes por shard' -> level=shards, waitForStatus=green, timeout=30s."
    )
    public Map<String, Object> elasticClusterHealth(
            @McpArg(name = "indexPattern", description = "Índice ou padrão opcional para escopar a saúde apenas a um subconjunto de índices. Se omitido, retorna a saúde global do cluster.")
            String indexPattern,
            @McpArg(name = "level", description = "Granularidade do retorno: 'cluster' (visão geral do cluster, default), 'indices' (detalha status por índice), 'shards' (detalha status por shard — mais verboso, use apenas quando necessário investigar alocação específica).")
            String level,
            @McpArg(name = "waitForStatus", description = "Bloqueia a resposta até o cluster atingir o status desejado: 'green' (todos os shards alocados), 'yellow' (shards primários alocados, réplicas pendentes), 'red' (shards primários não alocados). Use junto com timeout para evitar espera indefinida.")
            String waitForStatus,
            @McpArg(name = "timeout", description = "Tempo máximo de espera quando waitForStatus for informado. Após o timeout, retorna o status atual mesmo que não tenha atingido o alvo. Exemplos: 5s, 30s, 1m.")
            String timeout
    ) {
        try {
            return elasticSearchService.clusterHealth(indexPattern, level, waitForStatus, timeout);
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_cluster_health", ex);
            return this.errorResponse("elastic_cluster_health", ex);
        }
    }

    @McpTool(
            name = "elastic_index_stats",
            description = "Retorna estatísticas operacionais dos índices (docs, store, search, indexing etc.). " +
                    "USE para entender volume de dados, pressão de escrita/leitura e comportamento do armazenamento. " +
                    "NÃO USE para verificar saúde do cluster (prefira elastic_cluster_health) ou buscar documentos (prefira elastic_search_logs). " +
                    "RETORNA: por índice e totais agregados — contagem de documentos, tamanho em disco, número de queries/fetches, operações de indexação e merge, entre outras métricas operacionais. " +
                    "DICA: combine com elastic_cluster_health para um diagnóstico operacional completo. " +
                    "Exemplos de uso: " +
                    "(1) 'Quantos documentos e quanto espaço os índices de log do mcp-server ocupam?' -> indexPattern=mcp-server-logs-*, metrics=docs,store. " +
                    "(2) 'Mostre todas as estatísticas dos índices de pedidos' -> indexPattern=orders-logs-*. " +
                    "(3) 'Como está a pressão de busca e de indexação nos logs de hoje?' -> indexPattern=mcp-server-logs-*, metrics=search,indexing."
    )
    public Map<String, Object> elasticIndexStats(
            @McpArg(name = "indexPattern", description = "Padrão de índice/alias/data stream para obter estatísticas. Use elastic_list_indices para descobrir padrões disponíveis.")
            String indexPattern,
            @McpArg(name = "metrics", description = "Métricas desejadas separadas por vírgula. Se omitido, retorna todas. Opções: 'docs' (contagem de documentos e deletados), 'store' (tamanho em disco), 'search' (queries, fetches, tempo médio de busca), 'indexing' (operações de escrita, tempo de indexação), 'merge' (merges em andamento e concluídos), 'refresh' (refreshes e tempo gasto), 'flush' (flushes e tempo gasto). Exemplo: docs,store para volume; search,indexing para pressão de I/O.")
            String metrics
    ) {
        try {
            return elasticSearchService.indexStats(indexPattern, metrics);
        } catch (RuntimeException ex) {
            log.error("Erro ao executar tool elastic_index_stats", ex);
            return this.errorResponse("elastic_index_stats", ex);
        }
    }

    private Map<String, Object> errorResponse(String operation, RuntimeException ex) {
        return Map.of(
                "success", false,
                "operation", operation,
                "error", Objects.toString(ex.getMessage(), "Erro inesperado na consulta ao Elasticsearch.")
        );
    }
}
