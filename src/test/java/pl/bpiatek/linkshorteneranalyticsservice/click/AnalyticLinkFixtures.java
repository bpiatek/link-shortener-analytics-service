package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import pl.bpiatek.linkshorteneranalyticsservice.config.ClockConfiguration;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;

@Component
@Import(ClockConfiguration.class)
@ActiveProfiles("test")
class AnalyticLinkFixtures {

    private static final RowMapper<AnalyticsLink> ANALYTICS_LINK_ROW_MAPPER = (rs, rowNum) -> new AnalyticsLink(
            rs.getString("short_url"),
            rs.getString("link_id"),
            rs.getString("user_id"),
            rs.getBoolean("is_active")
    );

    private final Clock clock;
    private final SimpleJdbcInsert linkInsert;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    AnalyticLinkFixtures(Clock clock, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.clock = clock;
        this.linkInsert = new SimpleJdbcInsert(namedJdbcTemplate.getJdbcTemplate())
                .withTableName("analytics_links")
                .usingGeneratedKeyColumns("id");
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    AnalyticsLink getLinkByShortUrl(String shortUrl) {
        var sql = """
                SELECT al.short_url, al.link_id, al.user_id, al.is_active
                FROM analytics_links al
                WHERE al.short_url = :shortUrl""";

        var result = namedJdbcTemplate.query(sql, Map.of("shortUrl", shortUrl), ANALYTICS_LINK_ROW_MAPPER);

        return result.isEmpty() ? null : result.getFirst();
    }

    AnalyticsLink anAnalyticsLink(TestAnalyticsLink link) {
        var now = Timestamp.from(clock.instant());

        Map<String, Object> params = Map.of(
                "short_url", link.getShortUrl(),
                "link_id", link.getLinkId(),
                "user_id", link.getUserId(),
                "is_active", link.isActive(),
                "updated_at", now);

       linkInsert.execute(params);

       return getLinkByShortUrl(link.getShortUrl());
    }
}
