package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

class JdbcAnalyticsLinkRepository implements AnalyticsLinkRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final Clock clock;

    private static final RowMapper<AnalyticsLink> ANALYTICS_LINK_ROW_MAPPER = (rs, rowNum) -> new AnalyticsLink(
            rs.getString("short_url"),
            rs.getString("link_id"),
            rs.getString("user_id"),
            rs.getBoolean("is_active")
    );

    JdbcAnalyticsLinkRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.clock = clock;
    }

    @Override
    public Optional<AnalyticsLink> findByShortUrl(String shortUrl) {
        final String sql = """
           SELECT al.short_url, al.link_id, al.user_id, al.is_active
                FROM analytics_links al
                WHERE al.short_url = :shortUrl""";

        var results = namedJdbcTemplate.query(
                sql,
                Map.of("shortUrl", shortUrl),
                ANALYTICS_LINK_ROW_MAPPER
        );

        return results.stream().findFirst();
    }

    @Override
    public void save(AnalyticsLink link) {
        var now = Timestamp.from(clock.instant());
        final String sql = """
            INSERT INTO analytics_links (short_url, link_id, user_id, is_active, created_at, updated_at, deleted_at)
            VALUES (:shortUrl, :linkId, :userId, :isActive, :now, :now, NULL)
            ON CONFLICT (short_url) DO UPDATE SET
                deleted_at = NULL,
                updated_at = :now
            """;

        namedJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("shortUrl", link.shortUrl())
                        .addValue("linkId", link.linkId())
                        .addValue("userId", link.userId())
                        .addValue("isActive", link.isActive())
                        .addValue("now", now)
        );
    }

    @Override
    public void markAsDeleted(String shortUrl, Instant deletedAt) {
        var now = Timestamp.from(clock.instant());
        var sql = """
            UPDATE analytics_links
            SET deleted_at = :deleted_at, updated_at = :now
            WHERE short_url = :shortUrl
            """;

        namedJdbcTemplate.update(sql, Map.of("shortUrl", shortUrl, "now", now, "deleted_at", Timestamp.from(deletedAt)));
    }

    @Override
    public void updateStatus(String shortUrl, boolean isActive) {
        var now = Timestamp.from(clock.instant());
        final String sql = """
            UPDATE analytics_links
            SET is_active = :isActive, updated_at = :now
            WHERE short_url = :shortUrl""";

        namedJdbcTemplate.update(sql,
                new MapSqlParameterSource()
                        .addValue("shortUrl", shortUrl)
                        .addValue("isActive", isActive)
                        .addValue("now", now)
        );
    }
}