package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto.LinkClickEnrichedEvent;
import pl.bpiatek.linkshorteneranalyticsservice.enricher.EnricherService;

import java.time.Clock;

@Configuration
class ClickConfig {

    @Bean
    ClickEventConsumer clickEventConsumer(
            EnricherService enricherService,
            EnrichedClickRepository enrichedClickRepository,
            AnalyticsLinkRepository analyticsLinkRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        return new ClickEventConsumer(enricherService,
                enrichedClickRepository,
                analyticsLinkRepository,
                applicationEventPublisher);
    }

    @Bean
    EnrichedClickRepository jdbcEnrichedClickRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcEnrichedClickRepository(jdbcTemplate);
    }

    @Bean
    AnalyticsLinkRepository jdbcAnalyticsLinkRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        return new JdbcAnalyticsLinkRepository(jdbcTemplate, clock);
    }

    @Bean
    LinkLifecycleConsumer linkLifecycleConsumer(AnalyticsLinkRepository repository) {
        return new LinkLifecycleConsumer(repository);
    }

    @Bean
    EnrichedClickEventProducer enrichedClickEventProducer(
            KafkaTemplate<String, LinkClickEnrichedEvent> kafkaTemplate,
            @Value("${topic.analytics.enriched}") String topicName) {
        return new EnrichedClickEventProducer(kafkaTemplate, topicName);
    }

    @Bean
    KafkaIntegrationEvents kafkaIntegrationEvents(EnrichedClickEventProducer enrichedClickEventProducer) {
        return new KafkaIntegrationEvents(enrichedClickEventProducer);
    }
}
