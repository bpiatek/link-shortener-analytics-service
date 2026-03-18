package pl.bpiatek.linkshorteneranalyticsservice.enricher;

import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Configuration
class IpParserConfig {

    @Bean
    DatabaseReader databaseReader() throws IOException {
        var tempDbFile = extractDatabaseToTempFile();

        return new DatabaseReader.Builder(tempDbFile)
                .fileMode(Reader.FileMode.MEMORY_MAPPED)
                .withCache(new CHMCache())
                .build();
    }

    @Bean
    IpParser ipParser(DatabaseReader databaseReader) {
        return new IpParser(databaseReader);
    }

    private File extractDatabaseToTempFile() throws IOException {
        var resource = new ClassPathResource("GeoLite2-City.mmdb");
        var tempFile = File.createTempFile("GeoLite2-City", ".mmdb");
        tempFile.deleteOnExit();

        try (var inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }
}
