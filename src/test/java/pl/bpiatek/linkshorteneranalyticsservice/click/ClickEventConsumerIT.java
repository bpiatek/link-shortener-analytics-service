package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent;

import java.time.Duration;
import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;


@SpringBootTest
@ActiveProfiles("test")
class ClickEventConsumerIT implements WithFullInfrastructure {

    @Autowired
    private KafkaTemplate<String, LinkClickEvent> kafkaTemplate;

    @Value("${topic.link.clicks}")
    private String topicName;

    @Autowired
    private ClickFixtures clickFixtures;

    @Autowired
    private AnalyticLinkFixtures analyticLinkFixtures;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);
    }

    @Test
    void shouldSaveEnrichedClickEventToDatabase() {
        // given
        var clickedAt = LocalDateTime.parse("2025-08-04T10:11:30").toInstant(UTC);
        var shortUrl = "en78Se";
        var ipAddress = "35.242.177.6";
        var userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15";

        // needs to be created first
        var analyticsLink = analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder()
                .withIsActive(true)
                .withShortUrl(shortUrl)
                .build());

        var event = LinkClickEvent.newBuilder()
                .setClickedAt(Timestamp.newBuilder().setNanos(clickedAt.getNano()).build())
                .setIpAddress(ipAddress)
                .setUserAgent(userAgent)
                .setShortUrl(shortUrl)
                .build();

        // when
        kafkaTemplate.send(topicName, event);

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var clickFromDB = clickFixtures.getClickByShortUrl(shortUrl);
            assertThat(clickFromDB).isNotNull();
            assertSoftly(s -> {
                s.assertThat(clickFromDB.linkId()).isEqualTo(analyticsLink.getLinkId());
                s.assertThat(clickFromDB.userId()).isEqualTo(analyticsLink.getUserId());
                s.assertThat(clickFromDB.shortUrl()).isEqualTo(analyticsLink.getShortUrl());
                s.assertThat(Timestamp.newBuilder().setNanos(clickFromDB.clickedAt().getNano()).build())
                        .isEqualTo(event.getClickedAt());
                s.assertThat(clickFromDB.ipAddress()).isEqualTo(event.getIpAddress());
                s.assertThat(clickFromDB.userAgent()).isEqualTo(event.getUserAgent());
                s.assertThat(clickFromDB.countryCode()).isEqualTo("GB");
                s.assertThat(clickFromDB.cityName()).isEqualTo("London");
                s.assertThat(clickFromDB.asn()).isEqualTo("Unknown");
                s.assertThat(clickFromDB.deviceType()).isEqualTo("Desktop");
                s.assertThat(clickFromDB.osName()).isEqualTo("Mac OS");
                s.assertThat(clickFromDB.browserName()).isEqualTo("Safari");
            });
        });
    }
}