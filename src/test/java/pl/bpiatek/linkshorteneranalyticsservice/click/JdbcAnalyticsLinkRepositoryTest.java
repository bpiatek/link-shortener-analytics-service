package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static pl.bpiatek.linkshorteneranalyticsservice.click.TestAnalyticsLink.builder;

@JdbcTest
@Import({AnalyticLinkFixtures.class, JdbcAnalyticsLinkRepository.class})
@ActiveProfiles("test")
class JdbcAnalyticsLinkRepositoryTest implements WithPostgres {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AnalyticsLinkRepository repository;

    @Autowired
    AnalyticLinkFixtures analyticLinkFixtures;

    @MockitoBean
    Clock clock;

    private final Instant now = Instant.parse("2025-08-04T10:11:30Z");

    @BeforeEach
    void setup() {
        given(clock.instant()).willReturn(now);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM analytics_links");
    }

    @Test
    void shouldSaveAnalyticLinkInDatabase() {
        // given
        var shortUrl = "en78Se";
        var link = new AnalyticsLink(
                shortUrl,
                "12",
                "user-13",
                true
        );

        // when
        repository.save(link);

        // then
        var linkFromDB = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
        assertThat(linkFromDB).isNotNull();
        assertSoftly(s -> {
            s.assertThat(linkFromDB.getShortUrl()).isEqualTo(shortUrl);
            s.assertThat(linkFromDB.getLinkId()).isEqualTo(link.linkId());
            s.assertThat(linkFromDB.isActive()).isEqualTo(link.isActive());
            s.assertThat(linkFromDB.getUserId()).isEqualTo(link.userId());
            s.assertThat(linkFromDB.getCreatedAt()).isEqualTo(now);
            s.assertThat(linkFromDB.getUpdatedAt()).isEqualTo(now);
            s.assertThat(linkFromDB.getDeletedAt()).isNull();
        });
    }

    @Test
    void shouldNotUpdateUserIdOrLinkIdOnSameShortUrl() {
        // given
        var shortUrl = "en78Se";
        var firstLink = analyticLinkFixtures.anAnalyticsLink(
                builder()
                        .withShortUrl(shortUrl)
                        .withIsActive(true)
                        .build());

        var linkWithSameShortUrl = new AnalyticsLink(
                shortUrl,
                "100",
                "user-99",
                true
        );

        // when
        repository.save(linkWithSameShortUrl);

        // then
        var linkFromDB = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
        assertThat(linkFromDB).isNotNull();
        assertSoftly(s -> {
            s.assertThat(linkFromDB.isActive()).isTrue();
            s.assertThat(linkFromDB.getLinkId()).isEqualTo(firstLink.getLinkId());
            s.assertThat(linkFromDB.getUserId()).isEqualTo(firstLink.getUserId());
            s.assertThat(linkFromDB.getShortUrl()).isEqualTo(firstLink.getShortUrl());
        });
    }

    @Test
    void shouldNotUpdateIsActiveOnSavingSameShortUrlLink() {
        // given
        var shortUrl = "en78Se";
        var firstLink = analyticLinkFixtures.anAnalyticsLink(
                builder()
                        .withShortUrl(shortUrl)
                        .withIsActive(true)
                        .build());

        var linkWithSameShortUrl = new AnalyticsLink(
                shortUrl,
                firstLink.getLinkId(),
                firstLink.getUserId(),
                false
        );

        // when
        repository.save(linkWithSameShortUrl);

        // then
        var linkFromDB = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
        assertThat(linkFromDB).isNotNull();
        assertSoftly(s -> {
            s.assertThat(linkFromDB.isActive()).isTrue();
            s.assertThat(linkFromDB.getLinkId()).isEqualTo(firstLink.getLinkId());
            s.assertThat(linkFromDB.getUserId()).isEqualTo(firstLink.getUserId());
            s.assertThat(linkFromDB.getShortUrl()).isEqualTo(firstLink.getShortUrl());
        });
    }

    @Test
    void shouldUpdateUpdatedAtWhenTheSameEventIsSaved() {
        // given
        var instant = Instant.parse("2025-06-01T10:11:30Z");
        var shortUrl = "en78Se";
        var linkId = "100";
        var userId = "user-99";

        var firstLink = analyticLinkFixtures.anAnalyticsLink(
                builder()
                        .withShortUrl(shortUrl)
                        .withLinkId(linkId)
                        .withUserId(userId)
                        .withIsActive(true)
                        .withCreatedAt(instant)
                        .withUpdatedAt(instant)
                        .build());

        var linkWithSameShortUrl = new AnalyticsLink(
                shortUrl,
                linkId,
                userId,
                true
        );

        // when
        repository.save(linkWithSameShortUrl);

        // then
        var linkFromDB = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
        assertThat(linkFromDB).isNotNull();
        assertSoftly(s -> {
            s.assertThat(linkFromDB.isActive()).isTrue();
            s.assertThat(linkFromDB.getLinkId()).isEqualTo(firstLink.getLinkId());
            s.assertThat(linkFromDB.getUserId()).isEqualTo(firstLink.getUserId());
            s.assertThat(linkFromDB.getShortUrl()).isEqualTo(firstLink.getShortUrl());
            s.assertThat(linkFromDB.getUpdatedAt()).isEqualTo(now);
            s.assertThat(linkFromDB.getCreatedAt()).isEqualTo(firstLink.getCreatedAt());
        });
    }

    @Test
    void shouldFindAnalyticsLinkByShortUrl() {
        // given
        analyticLinkFixtures.anAnalyticsLink();
        var shortUrl = "sh87E2";
        var linkToFind = analyticLinkFixtures.anAnalyticsLink(builder().withShortUrl(shortUrl).build());

        // when
        var foundLink = repository.findByShortUrl(shortUrl);

        // then
        assertThat(foundLink).isPresent();
        var link = foundLink.get();
        assertSoftly(s -> {
            s.assertThat(link.linkId()).isEqualTo(linkToFind.getLinkId());
            s.assertThat(link.userId()).isEqualTo(linkToFind.getUserId());
            s.assertThat(link.shortUrl()).isEqualTo(linkToFind.getShortUrl());
            s.assertThat(link.isActive()).isFalse();
        });
    }

    @Test
    void shouldNotFindAnalyticsLinkByShortUrl() {
        // given
        analyticLinkFixtures.anAnalyticsLink();
        var shortUrl = "bogus";

        // when
        var foundLink = repository.findByShortUrl(shortUrl);

        // then
        assertThat(foundLink).isEmpty();
    }

    @Test
    void shouldMarkLinkAsDeleted() {
        // given
        var deletedAt = Instant.parse("2025-06-01T10:11:30Z");
        var link = analyticLinkFixtures.anAnalyticsLink(builder().withIsActive(true).build());

        // when
        repository.markAsDeleted(link.getShortUrl(), deletedAt);

        // then
        var linkFromDB = analyticLinkFixtures.getLinkByShortUrl(link.getShortUrl());
        assertThat(linkFromDB).isNotNull();
        assertSoftly(s -> {
            s.assertThat(linkFromDB.isActive()).isTrue();
            s.assertThat(linkFromDB.getDeletedAt()).isEqualTo(deletedAt);
        });
    }
}