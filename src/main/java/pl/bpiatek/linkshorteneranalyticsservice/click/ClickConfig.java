package pl.bpiatek.linkshorteneranalyticsservice.click;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@Configuration
class ClickConfig {

    @Bean
    ClickEventConsumer clickEventConsumer(
            EnricherService enricherService,
            EnrichedClickRepository enrichedClickRepository,
            AnalyticsLinkRepository analyticsLinkRepository) {
        return new ClickEventConsumer(enricherService, enrichedClickRepository, analyticsLinkRepository);
    }

    @Bean
    EnrichedClickRepository jdbcEnrichedClickRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcEnrichedClickRepository(jdbcTemplate);
    }

    @Bean
    AnalyticsLinkRepository jdbcAnalyticsLinkRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        return new JdbcAnalyticsLinkRepository(jdbcTemplate, clock);
    }

    @Bean
    EnricherService enricherService(UserAgentParser userAgentParser, IpParser ipParser) {
        return new EnricherService(userAgentParser, ipParser);
    }
}
