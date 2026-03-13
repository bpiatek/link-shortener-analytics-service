package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto.LinkClickEnrichedEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Component
@Profile("test")
public class TestKafkaConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(TestKafkaConsumer.class);

    private final BlockingQueue<ConsumerRecord<String, T>> records = new LinkedBlockingQueue<>();

    public void handle(ConsumerRecord<String, T> record) {
        log.info("Test consumer received record: {}", record.key());
        records.add(record);
    }

    // Retained for backward compatibility with tests that don't need filtering
    public ConsumerRecord<String, T> awaitRecord(long timeout, TimeUnit unit) throws InterruptedException {
        return awaitRecord(timeout, unit, record -> true);
    }

    // Master-level addition: Safely drains the queue until it finds the exact matching record
    public ConsumerRecord<String, T> awaitRecord(long timeout, TimeUnit unit, Predicate<ConsumerRecord<String, T>> filter) throws InterruptedException {
        var end = System.currentTimeMillis() + unit.toMillis(timeout);

        while (System.currentTimeMillis() < end) {
            var remaining = end - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }

            var record = records.poll(remaining, TimeUnit.MILLISECONDS);
            if (record != null) {
                if (filter.test(record)) {
                    log.debug("Found matching record: {}", record.key());
                    return record;
                } else {
                    log.warn("Discarding non-matching orphaned record: {}", record.key());
                }
            }
        }

        throw new IllegalStateException("No matching event received in the allotted time (" + timeout + " " + unit + ")");
    }

    public void reset() {
        records.clear();
    }
}