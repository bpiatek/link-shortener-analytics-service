package pl.bpiatek.linkshorteneranalyticsservice.click;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserAgentParser {

    private static final Logger log = LoggerFactory.getLogger(UserAgentParser.class);

    private final UserAgentAnalyzer userAgentAnalyzer;

    UserAgentParser(UserAgentAnalyzer userAgentAnalyzer) {
        this.userAgentAnalyzer = userAgentAnalyzer;
    }

    ParsedUserAgent parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new ParsedUserAgent(null, null, null);
        }

        try {
            var agent = userAgentAnalyzer.parse(userAgent);

            return new ParsedUserAgent(
                    getStringOrNull(agent.getValue(UserAgent.DEVICE_CLASS)),
                    getStringOrNull(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME)),
                    getStringOrNull(agent.getValue(UserAgent.AGENT_NAME))
            );
        } catch (Exception e) {
            log.warn("Failed to parse User-Agent string '{}': {}", userAgent, e.getMessage());
            return new ParsedUserAgent(null, null, null);
        }
    }

    private String getStringOrNull(String s) {
        return "Hacker".equals(s) ? null : s;
    }

}
