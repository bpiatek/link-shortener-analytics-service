package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent;
import pl.bpiatek.linkshorteneranalyticsservice.config.WithKafkaTestProducers;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent.*;

@SpringBootTest
@ActiveProfiles("test")
@WithKafkaTestProducers
class ClickEnhancedProducerTest implements WithFullInfrastructure {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestClickEnhancedConsumer testClickEnhancedConsumer;

    @Autowired
    private ClickFixtures clickFixtures;

    @Autowired
    private KafkaTemplate<String, LinkClickEvent> rawClickEventProducer;

    @Value("${topic.link.clicks}")
    String topicName;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.consumer.properties.specific.protobuf.value.type",
                () -> "pl.bpiatek.contracts.analytics.AnalyticsEventProto$LinkClickEnrichedEvent");
        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);
    }

    @AfterEach
    void clenUp() {
        jdbcTemplate.execute("DELETE FROM clicks");
        jdbcTemplate.execute("DELETE FROM analytics_links");
        testClickEnhancedConsumer.reset();
    }

    @Test
    void shouldPublishEnrichedClickEvent() throws InterruptedException {
        // given
        var now = Instant.parse("2025-08-04T10:11:30Z");
        var shortUrl = "shortUrl";
        clickFixtures.anAnalyticsLink(
                new ClickFixtures.AnalyticsLink(shortUrl, "linkId", "userId"));

        var linkClickEvent = newBuilder()
                .setShortUrl(shortUrl)
                .setIpAddress("127.0.0.1")
                .setClickedAt(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15")
                .build();

        // when
        rawClickEventProducer.send(topicName, linkClickEvent);

        // then
        var record = testClickEnhancedConsumer.awaitRecord(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertSoftly(s -> {
            var clickFromDb = clickFixtures.getClickByShortUrl(linkClickEvent.getShortUrl());
            s.assertThat(record.key()).isEqualTo(clickFromDb.id().toString());

            var event = record.value();
            s.assertThat(event.getClickId()).isEqualTo(clickFromDb.clickId());
            s.assertThat(event.getLinkId()).isEqualTo(clickFromDb.linkId());
            s.assertThat(event.getUserId()).isEqualTo(clickFromDb.userId());
            s.assertThat(event.getShortUrl()).isEqualTo(clickFromDb.shortUrl());
            s.assertThat(event.getTimestamp().getSeconds()).isEqualTo(clickFromDb.clickedAt().getEpochSecond());
            s.assertThat(event.getTimestamp().getNanos()).isEqualTo(clickFromDb.clickedAt().getNano());
            s.assertThat(event.getCountryCode()).isEqualTo("Unknown");
            s.assertThat(event.getCityName()).isEqualTo("Unknown");
            s.assertThat(event.getBrowserName()).isEqualTo("Safari");
            s.assertThat(event.getDeviceType()).isEqualTo("Desktop");
            s.assertThat(event.getOsName()).isEqualTo("Mac OS");
        });
    }
}