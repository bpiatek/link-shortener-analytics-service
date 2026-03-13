package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.google.protobuf.util.Timestamps;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkCreated;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkDeleted;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkUpdated;
import pl.bpiatek.linkshorteneranalyticsservice.config.IntegrationTest;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

class LinkLifecycleConsumerIT extends IntegrationTest {

    @Autowired
    private KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;

    @Value("${topic.link.lifecycle}")
    private String topicName;

    @Autowired
    private AnalyticLinkFixtures analyticLinkFixtures;

    @Test
    void shouldHandleLinkCreateEventAndSaveLink() {
        // given
        var shortUrl = "create-" + UUID.randomUUID().toString().substring(0, 5);
        var linkId = UUID.randomUUID().toString();
        var userId = "user-13";
        var longUrl = "https://example.com/some-long-url";

        // We capture 'now' to ensure the DB assigns a time very close to this
        var testStartTime = Instant.now();

        var linkCreated = LinkCreated.newBuilder()
                .setShortUrl(shortUrl)
                .setLongUrl(longUrl)
                .setUserId(userId)
                .setLinkId(linkId)
                .setIsActive(true)
                .setCreatedAt(Timestamps.fromMillis(testStartTime.toEpochMilli()))
                .build();

        var event = LinkLifecycleEvent.newBuilder()
                .setLinkCreated(linkCreated)
                .build();

        // when
        kafkaTemplate.send(topicName, event).join();

        // then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var linkByShortUrl = analyticLinkFixtures.getLinkByShortUrl(shortUrl);

            assertThat(linkByShortUrl).isNotNull();
            assertSoftly(s -> {
                s.assertThat(linkByShortUrl.getLinkId()).isEqualTo(linkId);
                s.assertThat(linkByShortUrl.getUserId()).isEqualTo(userId);
                s.assertThat(linkByShortUrl.isActive()).isTrue();
                s.assertThat(linkByShortUrl.getShortUrl()).isEqualTo(shortUrl);

                // Assert that the database correctly generated recent timestamps
                s.assertThat(linkByShortUrl.getCreatedAt()).isCloseTo(testStartTime, within(5, ChronoUnit.SECONDS));
                s.assertThat(linkByShortUrl.getUpdatedAt()).isCloseTo(testStartTime, within(5, ChronoUnit.SECONDS));
                s.assertThat(linkByShortUrl.getDeletedAt()).isNull();
            });
        });
    }

    @Test
    void shouldHandleLinkUpdatedEventAndUpdateLinkIsActiveField() {
        // given
        var creationTime = Instant.now().minusSeconds(3600); // 1 hour ago
        var testStartTime = Instant.now();
        var shortUrl = "update-" + UUID.randomUUID().toString().substring(0, 5);

        var alreadyInsertedLink = analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder()
                .withShortUrl(shortUrl)
                .withCreatedAt(creationTime)
                .withUpdatedAt(creationTime)
                .withIsActive(true)
                .build());

        var linkUpdated = LinkUpdated.newBuilder()
                .setShortUrl(shortUrl)
                .setLongUrl("https://example.com/updated")
                .setUserId(alreadyInsertedLink.getUserId())
                .setLinkId(alreadyInsertedLink.getLinkId())
                .setIsActive(false)
                .build();

        var event = LinkLifecycleEvent.newBuilder()
                .setLinkUpdated(linkUpdated)
                .build();

        // when
        kafkaTemplate.send(topicName, event).join();

        // then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var linkByShortUrl = analyticLinkFixtures.getLinkByShortUrl(shortUrl);

            assertThat(linkByShortUrl).isNotNull();
            assertSoftly(s -> {
                s.assertThat(linkByShortUrl.isActive()).isFalse();
                s.assertThat(linkByShortUrl.getLinkId()).isEqualTo(alreadyInsertedLink.getLinkId());

                // Assert the DB updated the modification time
                s.assertThat(linkByShortUrl.getUpdatedAt()).isCloseTo(testStartTime, within(5, ChronoUnit.SECONDS));
                s.assertThat(linkByShortUrl.getDeletedAt()).isNull();
            });
        });
    }

    @Test
    void shouldHandleLinkDeletedEventAndSoftDeleteLink() {
        // given
        var creationTime = Instant.now().minusSeconds(3600);
        var explicitDeletionTime = Instant.parse("2025-08-04T10:11:30Z");
        var shortUrl = "delete-" + UUID.randomUUID().toString().substring(0, 5);

        var alreadyInsertedLink = analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder()
                .withShortUrl(shortUrl)
                .withCreatedAt(creationTime)
                .withUpdatedAt(creationTime)
                .withIsActive(true)
                .build());

        var linkDeleted = LinkDeleted.newBuilder()
                .setShortUrl(shortUrl)
                .setUserId(alreadyInsertedLink.getUserId())
                .setLinkId(alreadyInsertedLink.getLinkId())
                .setDeletedAt(Timestamps.fromMillis(explicitDeletionTime.toEpochMilli()))
                .build();

        var event = LinkLifecycleEvent.newBuilder()
                .setLinkDeleted(linkDeleted)
                .build();

        // when
        kafkaTemplate.send(topicName, event).join();

        // then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var linkByShortUrl = analyticLinkFixtures.getLinkByShortUrl(shortUrl);

            assertThat(linkByShortUrl).isNotNull();
            assertSoftly(s -> {
                s.assertThat(linkByShortUrl.getLinkId()).isEqualTo(alreadyInsertedLink.getLinkId());
                s.assertThat(linkByShortUrl.getShortUrl()).isEqualTo(shortUrl);

                // Because handleLinkDeleted explicitly extracts deletedAt from the event and saves it,
                // this assertion remains exact.
                s.assertThat(linkByShortUrl.getDeletedAt()).isEqualTo(explicitDeletionTime);
            });
        });
    }

    @TestConfiguration
    static class TestProducerConfig {

        @Bean
        public KafkaTemplate<String, LinkLifecycleEvent> rawLifecycleEventKafkaTemplate(
                org.springframework.boot.autoconfigure.kafka.KafkaProperties kafkaProperties) {

            var props = kafkaProperties.buildProducerProperties(null);
            ProducerFactory<String, LinkLifecycleEvent> pf = new DefaultKafkaProducerFactory<>(props);

            return new KafkaTemplate<>(pf);
        }
    }
}