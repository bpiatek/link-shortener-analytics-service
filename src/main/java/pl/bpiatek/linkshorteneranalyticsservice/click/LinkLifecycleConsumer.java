package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkCreated;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkDeleted;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkUpdated;

import java.time.Instant;

class LinkLifecycleConsumer {

    private static final Logger log = LoggerFactory.getLogger(LinkLifecycleConsumer.class);

    private final AnalyticsLinkRepository repository;

    LinkLifecycleConsumer(AnalyticsLinkRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(
            topics = "${topic.link.lifecycle}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "linkLifecycleEventContainerFactory"
    )
    void consume(LinkLifecycleEvent event) {
        switch (event.getEventPayloadCase()) {
            case LINK_CREATED -> handleLinkCreated(event.getLinkCreated());
            case LINK_UPDATED -> handleLinkUpdated(event.getLinkUpdated());
            case LINK_DELETED -> handleLinkDeleted(event.getLinkDeleted());
            case EVENTPAYLOAD_NOT_SET -> log.warn("Received LinkLifecycleEvent with no payload set.");
            default -> log.warn("Received unknown event type in LinkLifecycleEvent: {}", event.getEventPayloadCase());
        }
    }

    private void handleLinkCreated(LinkCreated event) {
        log.info("Received LinkCreated event for short_url '{}'. Populating local link data store.", event.getShortUrl());
        var link = new AnalyticsLink(
                event.getShortUrl(),
                event.getLinkId(),
                event.getUserId(),
                true
        );
        repository.save(link);
    }

    private void handleLinkUpdated(LinkUpdated event) {
        repository.findByShortUrl(event.getShortUrl()).ifPresentOrElse(
                existingLink -> {
                    log.info("Received LinkUpdated event for short_url '{}'. Updating local data store.", event.getShortUrl());
                    repository.updateStatus(event.getShortUrl(), event.getIsActive());
                },
                () -> log.warn("Received LinkUpdated event for an unknown short_url '{}'. Ignoring.", event.getShortUrl())
        );
    }

    private void handleLinkDeleted(LinkDeleted event) {
        log.info("Received LinkDeleted event for short_url '{}'. Deactivating link in local data store.", event.getShortUrl());
        var deletedAt = convertManually(event.getDeletedAt());
        repository.markAsDeleted(event.getShortUrl(), deletedAt);
    }

    private Instant convertManually(Timestamp protoTimestamp) {
        long seconds = protoTimestamp.getSeconds();
        int nanos = protoTimestamp.getNanos();

        return Instant.ofEpochSecond(seconds, nanos);
    }
}
