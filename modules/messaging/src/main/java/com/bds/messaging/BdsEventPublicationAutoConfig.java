package com.bds.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

@Slf4j
@AutoConfiguration
@EnableScheduling
@ConditionalOnClass(IncompleteEventPublications.class)
public class BdsEventPublicationAutoConfig {

    static class EventRepublisher {
        private final ObjectProvider<IncompleteEventPublications> incomplete;
        private final Duration minAge;

        EventRepublisher(ObjectProvider<IncompleteEventPublications> incomplete, Duration minAge) {
            this.incomplete = incomplete;
            this.minAge = minAge;
        }

        @Scheduled(fixedDelayString = "${bds.messaging.events.republish-delay:30s}")
        void republish() {
            IncompleteEventPublications registry = incomplete.getIfAvailable();
            if (registry == null) return;   // registry 없는 컨텍스트면 조용히 스킵
            registry.resubmitIncompletePublicationsOlderThan(minAge);
        }
    }

    static class EventCleaner {
        private final ObjectProvider<CompletedEventPublications> completed;
        private final Duration retention;

        EventCleaner(ObjectProvider<CompletedEventPublications> completed, Duration retention) {
            this.completed = completed;
            this.retention = retention;
        }

        @Scheduled(cron = "${bds.messaging.events.cleanup-cron:0 0 4 * * *}")
        void cleanup() {
            CompletedEventPublications registry = completed.getIfAvailable();
            if (registry == null) return;
            registry.deletePublicationsOlderThan(retention);
            log.debug("[bds-messaging] completed event publications older than {} deleted", retention);
        }
    }

    @Bean
    EventRepublisher bdsEventRepublisher(ObjectProvider<IncompleteEventPublications> incomplete,
                                         @Value("${bds.messaging.events.republish-min-age:1m}") Duration minAge) {
        return new EventRepublisher(incomplete, minAge);
    }

    @Bean
    EventCleaner bdsEventCleaner(ObjectProvider<CompletedEventPublications> completed,
                                 @Value("${bds.messaging.events.retention:7d}") Duration retention) {
        return new EventCleaner(completed, retention);
    }
}