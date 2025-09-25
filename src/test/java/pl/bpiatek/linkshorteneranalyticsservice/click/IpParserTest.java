package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Traits;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.InetAddress;
import java.util.stream.Stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class IpParserTest {

    static Stream<TestCase> geoInfoProvider() {
        return Stream.of(
                new TestCase("8.8.8.8", "New York", "US", "Google LLC"),
                new TestCase("1.1.1.1", "Sydney", "AU", "Cloudflare"),
                new TestCase("203.0.113.1", "Unknown", "Unknown", "Unknown")
        );
    }

    @ParameterizedTest
    @MethodSource("geoInfoProvider")
    void shouldParseVariousIps(TestCase testCase) throws Exception {
        // given
        var dbReader = mock(DatabaseReader.class);
        var response = mock(CityResponse.class);

        setUpResponse(testCase, response, dbReader);
        var parser = new IpParser(dbReader);

        // when
        var result = parser.parse(testCase.ip);

        // then
        assertSoftly(s -> {
            s.assertThat(result.city()).isEqualTo(testCase.city);
            s.assertThat(result.country()).isEqualTo(testCase.country);
            s.assertThat(result.asnOrganization()).isEqualTo(testCase.asn);

        });
    }

    private static void setUpResponse(TestCase testCase, CityResponse response, DatabaseReader dbReader) throws IOException, GeoIp2Exception {
        if (!"Unknown".equals(testCase.city)
                || !"Unknown".equals(testCase.country)
                || !"Unknown".equals(testCase.asn)) {

            var country = mock(Country.class);
            given(country.getIsoCode()).willReturn(testCase.country);
            given(response.getCountry()).willReturn(country);

            var city = mock(City.class);
            given(city.getName()).willReturn(testCase.city);
            given(response.getCity()).willReturn(city);

            var traits = mock(Traits.class);
            given(traits.getAutonomousSystemOrganization()).willReturn(testCase.asn);
            given(response.getTraits()).willReturn(traits);

            given(dbReader.city(any(InetAddress.class))).willReturn(response);
        } else {
            // simulate DB returns no response
            given(dbReader.city(any(InetAddress.class))).willReturn(null);
        }
    }

    record TestCase(String ip, String city, String country, String asn) {}
}