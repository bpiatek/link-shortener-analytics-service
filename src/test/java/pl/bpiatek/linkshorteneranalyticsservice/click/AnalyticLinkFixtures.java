package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import pl.bpiatek.linkshorteneranalyticsservice.config.ClockConfiguration;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Component
@Import(ClockConfiguration.class)
@ActiveProfiles("test")
class AnalyticLinkFixtures {

    private static final RowMapper<TestAnalyticsLink> ANALYTICS_LINK_ROW_MAPPER = (rs, rowNum) -> TestAnalyticsLink.builder()
            .withShortUrl(rs.getString("short_url"))
            .withLinkId(rs.getString("link_id"))
            .withUserId(rs.getString("user_id"))
            .withIsActive(rs.getBoolean("is_active"))
            .withCreatedAt(rs.getTimestamp("created_at").toInstant())
            .withUpdatedAt(rs.getTimestamp("updated_at").toInstant())
            .withDeletedAt(rs.getTimestamp("deleted_at") != null ? rs.getTimestamp("deleted_at").toInstant() : null)
            .build();


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

    TestAnalyticsLink getLinkByShortUrl(String shortUrl) {
        var sql = """
                SELECT al.short_url, al.link_id, al.user_id, al.is_active, al.created_at, al.updated_at, al.deleted_at
                FROM analytics_links al
                WHERE al.short_url = :shortUrl""";

        var result = namedJdbcTemplate.query(sql, Map.of("shortUrl", shortUrl), ANALYTICS_LINK_ROW_MAPPER);

        return result.isEmpty() ? null : result.getFirst();
    }

    TestAnalyticsLink anAnalyticsLink() {
        return anAnalyticsLink(TestAnalyticsLink.builder().build());
    }

    TestAnalyticsLink anAnalyticsLink(TestAnalyticsLink link) {
        var now = Timestamp.from(clock.instant());

        var params = new MapSqlParameterSource()
                .addValue("short_url", link.getShortUrl())
                .addValue("link_id", link.getLinkId())
                .addValue("user_id", link.getUserId())
                .addValue("is_active", link.isActive())
                .addValue("created_at", toTimestampOrDefault(link.getCreatedAt(), now))
                .addValue("updated_at", toTimestampOrDefault(link.getUpdatedAt(), now))
                .addValue("deleted_at", toTimestampOrDefault(link.getDeletedAt(), null));

        linkInsert.execute(params);

       return getLinkByShortUrl(link.getShortUrl());
    }

    private Timestamp toTimestampOrDefault(Instant provided, Timestamp defaultValue) {
        return provided != null
                ? Timestamp.from(provided)
                : defaultValue;
    }
}
