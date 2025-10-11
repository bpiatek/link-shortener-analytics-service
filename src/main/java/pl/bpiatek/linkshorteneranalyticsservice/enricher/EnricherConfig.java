package pl.bpiatek.linkshorteneranalyticsservice.enricher;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EnricherConfig {

    @Bean
    EnricherService enricherService(UserAgentParser userAgentParser, IpParser ipParser) {
        return new EnricherService(userAgentParser, ipParser);
    }
}
