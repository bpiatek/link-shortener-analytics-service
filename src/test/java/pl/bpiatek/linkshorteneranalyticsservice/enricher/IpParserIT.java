package pl.bpiatek.linkshorteneranalyticsservice.enricher;

import com.maxmind.geoip2.DatabaseReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IpParserIT {

    private static IpParser parser;

    @BeforeAll
    static void setup() throws IOException {
        var dbFile = new ClassPathResource("GeoLite2-City.mmdb");

        assumeTrue(dbFile.exists(), "GeoLite2-City.mmdb not found, skipping integration tests");

        var reader = new DatabaseReader.Builder(dbFile.getInputStream()).build();
        parser = new IpParser(reader);
    }

    @Test
    void shouldResolveGoogleDnsIp() {
        // when
        var result = parser.parse("35.242.177.6");

        // then
        assertSoftly(s -> {
            s.assertThat(result.city()).isEqualTo("London");
            s.assertThat(result.asnOrganization()).isEqualTo("Unknown");
            s.assertThat(result.country()).isEqualTo("GB");
        });
    }
}
