package com.bds.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Duration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Slf4j
@AutoConfiguration
@EnableScheduling
@ConditionalOnClass(FailedEventPublications.class)
public class BdsEventPublicationAutoConfig {

    static class EventRepublisher {
        private final ObjectProvider<FailedEventPublications> failed;

        EventRepublisher(ObjectProvider<FailedEventPublications> failed) {
            this.failed = failed;
        }

        @Scheduled(fixedDelayString = "${bds.messaging.events.republish-delay:30s}")
        void republish() {
            FailedEventPublications registry = failed.getIfAvailable();
            if (registry == null) return;
            registry.resubmit(ResubmissionOptions.defaults().withMaxInFlight(50));
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
    EventRepublisher bdsEventRepublisher(ObjectProvider<FailedEventPublications> failed) {
        return new EventRepublisher(failed);
    }

    @Bean
    EventCleaner bdsEventCleaner(ObjectProvider<CompletedEventPublications> completed,
                                 @Value("${bds.messaging.events.retention:7d}") Duration retention) {
        return new EventCleaner(completed, retention);
    }
}