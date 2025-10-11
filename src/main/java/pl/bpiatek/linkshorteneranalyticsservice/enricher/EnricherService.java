package pl.bpiatek.linkshorteneranalyticsservice.enricher;



public class EnricherService {

    private final UserAgentParser userAgentParser;
    private final IpParser ipParser;

    EnricherService(UserAgentParser userAgentParser, IpParser ipParser) {
        this.userAgentParser = userAgentParser;
        this.ipParser = ipParser;
    }

    public EnrichedData enrich(String ipAddress, String userAgent) {
        var geoInfo = ipParser.parse(ipAddress);
        var parsedUserAgent = userAgentParser.parse(userAgent);

        return new EnrichedData(
                geoInfo.country(),
                geoInfo.city(),
                geoInfo.asnOrganization(),
                parsedUserAgent.deviceType(),
                parsedUserAgent.os(),
                parsedUserAgent.browser()
        );
    }
}