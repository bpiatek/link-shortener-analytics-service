package pl.bpiatek.linkshorteneranalyticsservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ClockConfig.class)
public class ClockConfiguration {
}
