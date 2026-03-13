package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent;
import pl.bpiatek.linkshorteneranalyticsservice.config.IntegrationTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

class ClickEventConsumerIT extends IntegrationTest {

    @Autowired
    private KafkaTemplate<String, LinkClickEvent> kafkaTemplate;

    @Value("${topic.link.clicks}")
    private String topicName;

    @Autowired
    private ClickFixtures clickFixtures;

    @Autowired
    private AnalyticLinkFixtures analyticLinkFixtures;

    @Test
    void shouldSaveEnrichedClickEventToDatabase() {
        // given
        var clickedAt = LocalDateTime.parse("2025-08-04T10:11:30").toInstant(ZoneOffset.UTC);

        var shortUrl = "en78Se-" + UUID.randomUUID().toString().substring(0, 5);
        var ipAddress = "35.242.177.6";
        var userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15";

        setupAnalyticsLink(shortUrl);
        var event = buildLinkClickEvent(shortUrl, ipAddress, userAgent, clickedAt);

        // when
        kafkaTemplate.send(topicName, event).join();

        // then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var clickFromDB = clickFixtures.getClickByShortUrl(shortUrl);

            assertThat(clickFromDB)
                    .as("Expected click event to be saved in the database but it was not found")
                    .isNotNull();

            assertSoftly(s -> {
                s.assertThat(clickFromDB.shortUrl()).isEqualTo(shortUrl);

                // Assert timestamps directly rather than rebuilding the Protobuf object in the assertion
                s.assertThat(clickFromDB.clickedAt().getEpochSecond()).isEqualTo(clickedAt.getEpochSecond());
                s.assertThat(clickFromDB.clickedAt().getNano()).isEqualTo(clickedAt.getNano());

                s.assertThat(clickFromDB.ipAddress()).isEqualTo(ipAddress);
                s.assertThat(clickFromDB.userAgent()).isEqualTo(userAgent);
                s.assertThat(clickFromDB.countryCode()).isEqualTo("GB");
                s.assertThat(clickFromDB.cityName()).isEqualTo("London");
                s.assertThat(clickFromDB.asn()).isEqualTo("Unknown");
                s.assertThat(clickFromDB.deviceType()).isEqualTo("Desktop");
                s.assertThat(clickFromDB.osName()).isEqualTo("Mac OS");
                s.assertThat(clickFromDB.browserName()).isEqualTo("Safari");
            });
        });
    }

    private void setupAnalyticsLink(String shortUrl) {
        analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder()
                .withIsActive(true)
                .withShortUrl(shortUrl)
                .build());
    }

    private LinkClickEvent buildLinkClickEvent(String shortUrl, String ipAddress, String userAgent, java.time.Instant clickedAt) {
        return LinkClickEvent.newBuilder()
                .setClickedAt(Timestamp.newBuilder()
                        .setSeconds(clickedAt.getEpochSecond())
                        .setNanos(clickedAt.getNano())
                        .build())
                .setIpAddress(ipAddress)
                .setUserAgent(userAgent)
                .setShortUrl(shortUrl)
                .build();
    }

    @TestConfiguration
    static class TestProducerConfig {

        @Bean
        public KafkaTemplate<String, LinkClickEvent> rawClickEventKafkaTemplate(KafkaProperties kafkaProperties) {

            var props = kafkaProperties.buildProducerProperties(null);

            ProducerFactory<String, LinkClickEvent> pf = new DefaultKafkaProducerFactory<>(props);

            return new KafkaTemplate<>(pf);
        }
    }
}