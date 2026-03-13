package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshorteneranalyticsservice.config.IntegrationTest;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ClickEnhancedProducerIT extends IntegrationTest {

    @Autowired
    private EnrichedClickEventProducer enrichedClickEventProducer;

    @Test
    void shouldPublishEnrichedClickEvent() throws InterruptedException {
        // given
        var now = Instant.parse("2025-08-04T10:11:30Z");
        var shortUrl = "shortUrl-" + UUID.randomUUID().toString().substring(0, 8);
        var enrichedClick = buildEnrichedClick(shortUrl, now);

        // when
        enrichedClickEventProducer.send(enrichedClick);

        // then
        var record = testEnrichedClickConsumer.awaitRecord(10, TimeUnit.SECONDS,
                rec -> shortUrl.equals(rec.value().getShortUrl()));

        assertThat(record).isNotNull();

        assertSoftly(s -> {
            s.assertThat(record.key()).isEqualTo(String.valueOf(enrichedClick.id()));

            var event = record.value();
            s.assertThat(event.getClickId()).isEqualTo(enrichedClick.clickId());
            s.assertThat(event.getLinkId()).isEqualTo(enrichedClick.linkId());
            s.assertThat(event.getUserId()).isEqualTo(enrichedClick.userId());
            s.assertThat(event.getShortUrl()).isEqualTo(enrichedClick.shortUrl());

            s.assertThat(event.getTimestamp().getSeconds()).isEqualTo(enrichedClick.clickedAt().getEpochSecond());
            s.assertThat(event.getTimestamp().getNanos()).isEqualTo(enrichedClick.clickedAt().getNano());

            s.assertThat(event.getCountryCode()).isEqualTo(enrichedClick.countryCode());
            s.assertThat(event.getBrowserName()).isEqualTo(enrichedClick.browserName());
            s.assertThat(event.getCityName()).isEqualTo(enrichedClick.cityName());
        });
    }

    private EnrichedClick buildEnrichedClick(String shortUrl, Instant now) {
        return new EnrichedClick(
                1L,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "user-1",
                shortUrl,
                now,
                "127.0.0.1",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15",
                "PL",
                "Warsaw",
                "52.2297",
                "21.0122",
                "AS12345",
                "Desktop",
                "Mac OS",
                "Safari"
        );
    }
}