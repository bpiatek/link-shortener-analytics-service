package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;

@Component
@ActiveProfiles("test")
public class ClickFixtures {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final SimpleJdbcInsert clickInsert;
    private final SimpleJdbcInsert analyticsLinkInsert;
    private final Clock clock;

    ClickFixtures(NamedParameterJdbcTemplate namedJdbcTemplate, Clock clock) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.clickInsert = new SimpleJdbcInsert(namedJdbcTemplate.getJdbcTemplate())
                .withTableName("clicks")
                .usingGeneratedKeyColumns("id");
        this.analyticsLinkInsert = new SimpleJdbcInsert(namedJdbcTemplate.getJdbcTemplate())
                .withTableName("analytics_links");
        this.clock = clock;
    }

    AnalyticsLink anAnalyticsLink(AnalyticsLink analyticsLink) {
        var params = new MapSqlParameterSource()
                .addValue("short_url", analyticsLink.shortUrl)
                .addValue("link_id", analyticsLink.linkId)
                .addValue("user_id", analyticsLink.userId)
                .addValue("is_active", true)
                .addValue("created_at", Timestamp.from(clock.instant()))
                .addValue("updated_at", Timestamp.from(clock.instant()));

        analyticsLinkInsert.execute(params);

        return  analyticsLink;
    }

    public record AnalyticsLink(String shortUrl, String linkId, String userId) {}

    EnrichedClick aClick(EnrichedClick click) {
        var now = clock.instant();

        var params = new MapSqlParameterSource()
                .addValue("click_id", click.clickId())
                .addValue("link_id", click.linkId())
                .addValue("user_id", click.userId())
                .addValue("short_url", click.shortUrl())
                .addValue("clicked_at", Timestamp.from(click.clickedAt()))
                .addValue("ip_address", click.ipAddress())
                .addValue("country_code", click.countryCode())
                .addValue("city_name", click.cityName())
                .addValue("asn", click.asn())
                .addValue("user_agent", click.userAgent())
                .addValue("device_type", click.deviceType())
                .addValue("os_name", click.osName())
                .addValue("browser_name", click.browserName())
                .addValue("created_at", Timestamp.from(now))
                .addValue("updated_at", Timestamp.from(now));

        var id = clickInsert.executeAndReturnKey(params).longValue();
        return click.withId(id);
    }

    EnrichedClick getClickById(Long id) {
        var sql = """
                SELECT c.id, c.click_id, c.link_id, c.user_id, c.link_short_url,
                    c.clicked_at, c.ip_address, c.country_code, c.city_name, c.asn, c.user_agent,
                    c.device_type, c.os_name, c.browser_name
                FROM clicks c
                WHERE c.id = :id""";

        var result = namedJdbcTemplate.query(sql, Map.of("id", id), ENRICHED_CLICK_ROW_MAPPER);

        return result.isEmpty() ? null : result.getFirst();
    }

    EnrichedClick getClickByShortUrl(String shortUrl) {
        var sql = """
                SELECT c.id, c.click_id, c.link_id, c.user_id, c.link_short_url,
                    c.clicked_at, c.ip_address, c.country_code, c.city_name, c.asn, c.user_agent,
                    c.device_type, c.os_name, c.browser_name
                FROM clicks c
                WHERE c.link_short_url = :shortUrl""";

        var result = namedJdbcTemplate.query(sql, Map.of("shortUrl", shortUrl), ENRICHED_CLICK_ROW_MAPPER);

        return result.isEmpty() ? null : result.getFirst();
    }

    private static final RowMapper<EnrichedClick> ENRICHED_CLICK_ROW_MAPPER = (rs, rowNum) -> new EnrichedClick(
            rs.getLong("id"),
            rs.getString("click_id"),
            rs.getString("link_id"),
            rs.getString("user_id"),
            rs.getString("link_short_url"),
            rs.getTimestamp("clicked_at").toInstant(),
            rs.getString("ip_address"),
            rs.getString("user_agent"),
            rs.getString("country_code"),
            rs.getString("city_name"),
            rs.getString("asn"),
            rs.getString("device_type"),
            rs.getString("os_name"),
            rs.getString("browser_name")
    );
}
