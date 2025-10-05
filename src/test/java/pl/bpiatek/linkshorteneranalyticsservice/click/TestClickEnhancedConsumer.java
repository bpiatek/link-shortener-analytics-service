package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto.LinkClickEnrichedEvent;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Profile("test")
public class TestClickEnhancedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TestClickEnhancedConsumer.class);

    private CountDownLatch latch = new CountDownLatch(1);
    private ConsumerRecord<String, LinkClickEnrichedEvent> payload;

    @KafkaListener(
            topics = "${topic.analytics.enriched}",
            groupId = "test-consumer-group",
            autoStartup = "true"
    )
    public void receive(ConsumerRecord<String, LinkClickEnrichedEvent> consumerRecord) {
        log.info("Test consumer received message with key: {}", consumerRecord.key());
        payload = consumerRecord;
        latch.countDown();
    }

    public ConsumerRecord<String, LinkClickEnrichedEvent> awaitRecord(long timeout, TimeUnit unit) throws InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new IllegalStateException("No message received in the allotted time");
        }
        return payload;
    }

    public void reset() {
        latch = new CountDownLatch(1);
        payload = null;
    }
}