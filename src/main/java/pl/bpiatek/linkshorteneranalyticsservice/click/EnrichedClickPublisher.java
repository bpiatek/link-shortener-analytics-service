package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

class EnrichedClickPublisher {

    private final EnrichedClickEventProducer enrichedClickEventProducer;

    EnrichedClickPublisher(EnrichedClickEventProducer enrichedClickEventProducer) {
        this.enrichedClickEventProducer = enrichedClickEventProducer;
    }

    @Async
    @EventListener
    void handleClickEnrichedEvent(ClickEnrichedApplicationEvent event) {
        enrichedClickEventProducer.send(event.enrichedClick());
    }
}


