package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pl.bpiatek.linkshorteneranalyticsservice.config.ClockConfiguration;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@JdbcTest
@Import({ClickFixtures.class, JdbcEnrichedClickRepository.class, ClockConfiguration.class})
@ActiveProfiles("test")
class JdbcEnrichedClickRepositoryTest implements WithPostgres {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EnrichedClickRepository repository;

    @Autowired
    ClickFixtures clickFixtures;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM clicks");
    }

    @Test
    void shouldSaveEnrichedClick() {
        // given
        var clickedAt = LocalDateTime.parse("2025-08-04T10:11:30").toInstant(UTC);
        var enrichedClick = new EnrichedClick(
                null,
                UUID.randomUUID().toString(),
                "12",
                "user-13",
                "en78Se",
                clickedAt,
                "35.242.177.6",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15\n (KHTML, like Gecko) Version/26.0 Safari/605.1.15",
                "GB",
                "London",
                "Unknown",
                "Desktop",
                "Mac OS",
                "Safari");

        // when
        var savedEvent = repository.save(enrichedClick);

        // then
        EnrichedClick fromDb = clickFixtures.getClickById(savedEvent.id());
        assertThat(savedEvent).isNotNull();
        assertSoftly(s -> {
            s.assertThat(fromDb.clickId()).isEqualTo(enrichedClick.clickId());
            s.assertThat(fromDb.linkId()).isEqualTo(enrichedClick.linkId());
            s.assertThat(fromDb.userId()).isEqualTo(enrichedClick.userId());
            s.assertThat(fromDb.shortUrl()).isEqualTo(enrichedClick.shortUrl());
            s.assertThat(fromDb.clickedAt()).isEqualTo(enrichedClick.clickedAt());
            s.assertThat(fromDb.ipAddress()).isEqualTo(enrichedClick.ipAddress());
            s.assertThat(fromDb.countryCode()).isEqualTo(enrichedClick.countryCode());
            s.assertThat(fromDb.cityName()).isEqualTo(enrichedClick.cityName());
            s.assertThat(fromDb.asn()).isEqualTo(enrichedClick.asn());
            s.assertThat(fromDb.userAgent()).isEqualTo(enrichedClick.userAgent());
            s.assertThat(fromDb.deviceType()).isEqualTo(enrichedClick.deviceType());
            s.assertThat(fromDb.osName()).isEqualTo(enrichedClick.osName());
            s.assertThat(fromDb.browserName()).isEqualTo(enrichedClick.browserName());
        });
    }
}