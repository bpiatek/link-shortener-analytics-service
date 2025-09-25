package pl.bpiatek.linkshorteneranalyticsservice.click;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
class IpParserConfig {

    @Bean
    DatabaseReader databaseReader() throws IOException {
        var cityResource = new ClassPathResource("GeoLite2-City.mmdb");
        return   new DatabaseReader.Builder(cityResource.getInputStream()).build();
    }

    @Bean
    IpParser ipParser(DatabaseReader databaseReader) {
        return new IpParser(databaseReader);
    }
}
