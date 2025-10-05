package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto.LinkClickEnrichedEvent;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.nio.charset.StandardCharsets.UTF_8;

class EnrichedClickEventProducer {

    private static final Logger log = LoggerFactory.getLogger(EnrichedClickEventProducer.class);

    private static final String SOURCE_HEADER_VALUE = "analytics-service";

    private final KafkaTemplate<String, LinkClickEnrichedEvent> kafkaTemplate;
    private final String topicName;

    EnrichedClickEventProducer(KafkaTemplate<String, LinkClickEnrichedEvent> kafkaTemplate, String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    void send(EnrichedClick click) {
        var eventToSend = LinkClickEnrichedEvent.newBuilder()
                .setClickId(click.clickId())
                .setLinkId(click.linkId())
                .setUserId(click.userId())
                .setShortUrl(click.shortUrl())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(click.clickedAt().getEpochSecond())
                        .setNanos(click.clickedAt().getNano())
                        .build())
                .setCityName(click.cityName())
                .setCountryCode(click.countryCode())
                .setDeviceType(click.deviceType())
                .setOsName(click.osName())
                .setBrowserName(click.browserName())
                .build();

        var producerRecord = new ProducerRecord<>(topicName, String.valueOf(click.id()), eventToSend);
        producerRecord.headers().add(new RecordHeader("trace-id", UUID.randomUUID().toString().getBytes(UTF_8)));
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        try {
            var result = kafkaTemplate.send(producerRecord).get();
            log.info("Successfully published LinkClickEnrichedEvent for click_id: {} to partition: {} offset: {}",
                    click.clickId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to publish LinkClickEnrichedEvent for click_id: {}. Reason: {}",
                    click.clickId(),
                    e.getMessage());
        }
    }
}
