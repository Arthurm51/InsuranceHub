package br.com.insurehub.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PolicyExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(PolicyExpirationJob.class);

    private final PolicyService service;

    public PolicyExpirationJob(PolicyService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    public void run() {
        int expired = service.expireOverdue();
        if (expired > 0) {
            log.info("{} apólice(s) marcada(s) como EXPIRED", expired);
        }
    }
}
