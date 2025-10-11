package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent;
import pl.bpiatek.linkshorteneranalyticsservice.enricher.EnricherService;

import java.time.Instant;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final EnricherService enricherService;
    private final EnrichedClickRepository repository;
    private final AnalyticsLinkRepository analyticsLinkRepository;
    private final ApplicationEventPublisher eventPublisher;

    ClickEventConsumer(EnricherService enricherService, EnrichedClickRepository repository,
                       AnalyticsLinkRepository analyticsLinkRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.enricherService = enricherService;
        this.repository = repository;
        this.analyticsLinkRepository = analyticsLinkRepository;
        this.eventPublisher = eventPublisher;
    }


    @KafkaListener(
            topics = "${topic.link.clicks}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "linkClickEventContainerFactory"
    )
    public void consumeLinkLifecycleEvent(LinkClickEvent event) {
        log.debug("Received click event: {}", event);
        analyticsLinkRepository.findByShortUrl(event.getShortUrl())
                .ifPresentOrElse(linkInfo -> {

                    log.debug("Event clicked at: {}", event.getClickedAt());
            var signature = event.getShortUrl()
                    + event.getIpAddress()
                    + event.getUserAgent()
                    + event.getClickedAt().getSeconds();

            var enrichedData = enricherService.enrich(event.getIpAddress(), event.getUserAgent());
            var clickId = UUID.nameUUIDFromBytes(signature.getBytes(UTF_8)).toString();

            var enrichedClick = new EnrichedClick(
                    null,
                    clickId,
                    linkInfo.linkId(),
                    linkInfo.userId(),
                    event.getShortUrl(),
                    Instant.ofEpochSecond(event.getClickedAt().getSeconds(), event.getClickedAt().getNanos()),
                    event.getIpAddress(),
                    event.getUserAgent(),
                    enrichedData.countryCode(),
                    enrichedData.cityName(),
                    enrichedData.asn(),
                    enrichedData.deviceType(),
                    enrichedData.osName(),
                    enrichedData.browserName()
            );

            try {
                var enrichedToSend = repository.save(enrichedClick);
                eventPublisher.publishEvent(new ClickEnrichedApplicationEvent(enrichedToSend));
                log.info("Successfully saved enriched click data for click_id: {}", clickId);
            } catch (DataIntegrityViolationException e) {
                log.warn("Duplicate click event detected and ignored for click_id: {}", clickId);
            }

        }, () -> {
            // This happens if the click event arrives before the link creation event has been processed
            log.warn("Received click for unknown or inactive short_url '{}'. Dropping event.", event.getShortUrl());
        });
    }
}