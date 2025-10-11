package pl.bpiatek.linkshorteneranalyticsservice.enricher;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class UserAgentConfig {

    @Bean
    UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer
                .newBuilder()
                .hideMatcherLoadStats()
                .immediateInitialization()
                .withCache(1000)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.DEVICE_CLASS)
                .build();
    }

    @Bean
    UserAgentParser userAgentParser(UserAgentAnalyzer userAgentAnalyzer) {
        return new UserAgentParser(userAgentAnalyzer);
    }
}
