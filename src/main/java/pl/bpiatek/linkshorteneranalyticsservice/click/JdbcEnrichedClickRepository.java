package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.sql.Timestamp;
import java.util.Map;

class JdbcEnrichedClickRepository implements EnrichedClickRepository {

    private final SimpleJdbcInsert linkInsert;

    JdbcEnrichedClickRepository(JdbcTemplate jdbcTemplate) {
        this.linkInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("clicks")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public EnrichedClick save(EnrichedClick enrichedClick) {

        Map<String, Object> params = Map.ofEntries(
                Map.entry("click_id", enrichedClick.clickId()),
                Map.entry("link_id", enrichedClick.linkId()),
                Map.entry("user_id", enrichedClick.userId()),
                Map.entry("link_short_url", enrichedClick.shortUrl()),
                Map.entry("clicked_at", Timestamp.from(enrichedClick.clickedAt())),
                Map.entry("ip_address", enrichedClick.ipAddress()),
                Map.entry("country_code", enrichedClick.countryCode()),
                Map.entry("city_name", enrichedClick.cityName()),
                Map.entry("asn", enrichedClick.asn()),
                Map.entry("user_agent", enrichedClick.userAgent()),
                Map.entry("device_type", enrichedClick.deviceType()),
                Map.entry("os_name", enrichedClick.osName()),
                Map.entry("browser_name", enrichedClick.browserName())
        );

        var key = linkInsert.executeAndReturnKey(params);

        return enrichedClick.withId(key.longValue());
    }
}
