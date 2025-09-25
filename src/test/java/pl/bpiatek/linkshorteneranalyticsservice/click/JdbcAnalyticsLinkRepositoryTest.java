package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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
    }

    @Test
    void shouldUpdateAnalyticLinkOnSameShortUrl() {
        // given
        var shortUrl = "en78Se";
        var firstLink = analyticLinkFixtures.anAnalyticsLink(
                TestAnalyticsLink.builder()
                        .withShortUrl(shortUrl)
                        .build());

        var linkWithSameShortUrl = new AnalyticsLink(
                shortUrl,
                "100",
                "user-99",
                false
        );

        // when
        repository.save(linkWithSameShortUrl);

        // then
        var linkFromDB = analyticLinkFixtures.getLinkByShortUrl(shortUrl);
        assertThat(linkFromDB).isNotNull();
        assertSoftly(s -> {
            s.assertThat(linkFromDB.linkId()).isEqualTo(linkWithSameShortUrl.linkId());
            s.assertThat(linkFromDB.userId()).isEqualTo(linkWithSameShortUrl.userId());
            s.assertThat(linkFromDB.isActive()).isEqualTo(linkWithSameShortUrl.isActive());
            s.assertThat(linkFromDB.shortUrl()).isEqualTo(firstLink.shortUrl());
        });
    }

    @Test
    void shouldFindAnalyticsLinkByShortUrl() {
        // given
        analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder().build());
        var shortUrl = "sh87E2";
        var linkToFind = analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder().withShortUrl(shortUrl).build());

        // when
        var foundLink = repository.findByShortUrl(shortUrl);

        // then
        assertThat(foundLink).isPresent();
        assertThat(foundLink.get()).isEqualTo(linkToFind);
    }

    @Test
    void shouldNotFindAnalyticsLinkByShortUrl() {
        // given
        analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder().build());
        var shortUrl = "bogus";

        // when
        var foundLink = repository.findByShortUrl(shortUrl);

        // then
        assertThat(foundLink).isEmpty();
    }

    @Test
    void shouldDeactivateLink() {
        // given
        var link = analyticLinkFixtures.anAnalyticsLink(TestAnalyticsLink.builder().withIsActive(true).build());

        // when
        repository.deactivate(link.shortUrl());

        // then
        var linkFromDB = analyticLinkFixtures.getLinkByShortUrl(link.shortUrl());
        assertThat(linkFromDB).isNotNull();
        assertThat(linkFromDB.isActive()).isFalse();
    }
}