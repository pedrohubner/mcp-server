package com.pedrohubner.mcpserver.resources;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "resources.refresh.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ResourcesRefreshScheduler {
    private final ResourcesRefreshService resourcesRefreshService;

    @Scheduled(
            fixedDelayString = "${resources.refresh.scheduler.fixed-delay:PT3M}",
            initialDelayString = "${resources.refresh.scheduler.initial-delay:PT10S}"
    )
    public void refresh() {
        log.info("Iniciando refresh agendado de resources.");
        resourcesRefreshService.refresh("scheduler");
    }
}
