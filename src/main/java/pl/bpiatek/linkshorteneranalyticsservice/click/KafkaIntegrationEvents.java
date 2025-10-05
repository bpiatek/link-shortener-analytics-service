package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

class KafkaIntegrationEvents {

    private final EnrichedClickEventProducer enrichedClickEventProducer;

    KafkaIntegrationEvents(EnrichedClickEventProducer enrichedClickEventProducer) {
        this.enrichedClickEventProducer = enrichedClickEventProducer;
    }

    @Async
    @EventListener
    void handleClickEnrichedEvent(ClickEnrichedApplicationEvent event) {
        enrichedClickEventProducer.send(event.enrichedClick());
    }
}


