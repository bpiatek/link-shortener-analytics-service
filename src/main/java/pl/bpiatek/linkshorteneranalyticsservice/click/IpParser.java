package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

class IpParser {

    private static final Logger log = LoggerFactory.getLogger(IpParser.class);
    private static final String UNKNOWN = "Unknown";

    private final DatabaseReader cityReader;

    IpParser(DatabaseReader cityReader) {
        this.cityReader = cityReader;
    }

    EnrichedGeoInfo parse(String ipAddress) {
        var countryCode = UNKNOWN;
        var cityName = UNKNOWN;
        var asnOrganization = UNKNOWN;

        if (ipAddress != null && !ipAddress.isBlank()) {
            try {
                InetAddress ip = InetAddress.getByName(ipAddress);
                var cityResponse = cityReader.city(ip);

                if (cityResponse != null) {
                    if (cityResponse.getCountry() != null && cityResponse.getCountry().getIsoCode() != null) {
                        countryCode = cityResponse.getCountry().getIsoCode();
                    }
                    if (cityResponse.getCity() != null && cityResponse.getCity().getName() != null) {
                        cityName = cityResponse.getCity().getName();
                    }
                    if (cityResponse.getTraits() != null && cityResponse.getTraits().getAutonomousSystemOrganization() != null) {
                        asnOrganization = cityResponse.getTraits().getAutonomousSystemOrganization();
                    }
                }
            } catch (IOException | GeoIp2Exception e) {
                log.warn("Failed to enrich IP address '{}': {}", ipAddress, e.getMessage());
            }
        }
        return new EnrichedGeoInfo(cityName, countryCode, asnOrganization);
    }
}
