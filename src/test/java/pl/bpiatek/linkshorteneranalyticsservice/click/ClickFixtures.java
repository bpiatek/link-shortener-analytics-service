package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

@Component
@ActiveProfiles("test")
class ClickFixtures {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    ClickFixtures(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
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
