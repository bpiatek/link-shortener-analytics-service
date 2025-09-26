package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.google.protobuf.util.Timestamps;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkCreated;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkDeleted;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkUpdated;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static pl.bpiatek.linkshorteneranalyticsservice.click.TestAnalyticsLink.builder;

@SpringBootTest
@ActiveProfiles("test")
class LinkLifecycleConsumerIT implements WithFullInfrastructure {

    @Autowired
    private KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;

    @Value("${topic.link.lifecycle}")
    private String topicName;

    @Autowired
    private AnalyticLinkFixtures analyticLinkFixtures;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    Clock clock;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM analytics_links");
    }

    @Test
    void shouldHandleLinkCreateEventAndSaveLink() {
        // given
        var now = Instant.parse("2025-08-04T10:11:30Z");
        given(clock.instant()).willReturn(now);

        var shortUrl = "en78Se";
        var linkId = "12";
        var userId = "user-13";
        var longUrl = "https://example.com/some-long-url";

        var linkCreated = LinkCreated.newBuilder()
                .setShortUrl(shortUrl)
                .setLongUrl(longUrl)
                .setUserId(userId)
                .setLinkId(linkId)
                .setCreatedAt(Timestamps.fromMillis(now.toEpochMilli()))
                .build();

        var event = LinkLifecycleEvent.newBuilder()
                .setLinkCreated(linkCreated)
                .build();

        // when
        kafkaTemplate.send(topicName, event);

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var linkByShortUrl = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
            assertThat(linkByShortUrl).isNotNull();
            assertSoftly(s -> {
                s.assertThat(linkByShortUrl.getLinkId()).isEqualTo(linkId);
                s.assertThat(linkByShortUrl.getUserId()).isEqualTo(userId);
                s.assertThat(linkByShortUrl.isActive()).isTrue();
                s.assertThat(linkByShortUrl.getShortUrl()).isEqualTo(shortUrl);
                s.assertThat(linkByShortUrl.getCreatedAt()).isEqualTo(now);
                s.assertThat(linkByShortUrl.getUpdatedAt()).isEqualTo(now);
                s.assertThat(linkByShortUrl.getDeletedAt()).isNull();
            });
        });
    }

    @Test
    void shouldHandleLinkUpdatedEventAndUpdateLinkIsActiveField() {
        // given
        var creationTime = LocalDateTime.parse("2025-03-01T08:11:30").toInstant(UTC);
        var updateTime = LocalDateTime.parse("2025-08-04T10:11:30").toInstant(UTC);
        given(clock.instant()).willReturn(updateTime);

        var shortUrl = "en78Se";
        var alreadyInsertedLink = analyticLinkFixtures.anAnalyticsLink(builder()
                .withShortUrl(shortUrl)
                .withCreatedAt(creationTime)
                .withUpdatedAt(creationTime)
                .build());

        var linkUpdated = LinkUpdated.newBuilder()
                .setShortUrl(shortUrl)
                .setLongUrl(alreadyInsertedLink.getLinkId())
                .setUserId(alreadyInsertedLink.getUserId())
                .setLinkId(alreadyInsertedLink.getLinkId())
                .setIsActive(false)
                .build();

        var event = LinkLifecycleEvent.newBuilder()
                .setLinkUpdated(linkUpdated)
                .build();

        // when
        kafkaTemplate.send(topicName, event);

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var linkByShortUrl = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
            assertThat(linkByShortUrl).isNotNull();
            assertSoftly(s -> {
                s.assertThat(linkByShortUrl.isActive()).isFalse();
                s.assertThat(linkByShortUrl.getLinkId()).isEqualTo(alreadyInsertedLink.getLinkId());
                s.assertThat(linkByShortUrl.getUserId()).isEqualTo(alreadyInsertedLink.getUserId());
                s.assertThat(linkByShortUrl.getShortUrl()).isEqualTo(shortUrl);
                s.assertThat(linkByShortUrl.getCreatedAt()).isEqualTo(creationTime);
                s.assertThat(linkByShortUrl.getUpdatedAt()).isEqualTo(updateTime);
                s.assertThat(linkByShortUrl.getDeletedAt()).isNull();
            });
        });
    }

    @Test
    void shouldHandleLinkDeletedEventAndSoftDeleteLink() {
        // given
        var creationTime = Instant.parse("2025-03-01T08:11:30Z");
        var deletionTime = Instant.parse("2025-08-04T10:11:30Z");

        var shortUrl = "en78Se";
        given(clock.instant()).willReturn(creationTime);
        var alreadyInsertedLink = analyticLinkFixtures.anAnalyticsLink(builder()
                .withShortUrl(shortUrl)
                .withCreatedAt(creationTime)
                .withUpdatedAt(creationTime)
                .withIsActive(true)
                .build());

        given(clock.instant()).willReturn(deletionTime.plusSeconds(1));
        var linkDeleted = LinkDeleted.newBuilder()
                .setShortUrl(shortUrl)
                .setUserId(alreadyInsertedLink.getUserId())
                .setLinkId(alreadyInsertedLink.getLinkId())
                .setDeletedAt(Timestamps.fromMillis(deletionTime.toEpochMilli()))
                .build();

        var event = LinkLifecycleEvent.newBuilder()
                .setLinkDeleted(linkDeleted)
                .build();

        // when
        kafkaTemplate.send(topicName, event);

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var linkByShortUrl = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
            assertThat(linkByShortUrl).isNotNull();
            assertSoftly(s -> {
                s.assertThat(linkByShortUrl.isActive()).isTrue();
                s.assertThat(linkByShortUrl.getLinkId()).isEqualTo(alreadyInsertedLink.getLinkId());
                s.assertThat(linkByShortUrl.getUserId()).isEqualTo(alreadyInsertedLink.getUserId());
                s.assertThat(linkByShortUrl.getShortUrl()).isEqualTo(shortUrl);
                s.assertThat(linkByShortUrl.getCreatedAt()).isEqualTo(creationTime);
                s.assertThat(linkByShortUrl.getUpdatedAt()).isEqualTo(deletionTime.plusSeconds(1));
                s.assertThat(linkByShortUrl.getDeletedAt()).isEqualTo(deletionTime);
            });
        });
    }
}