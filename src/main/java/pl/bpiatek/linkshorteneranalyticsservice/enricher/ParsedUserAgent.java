package pl.bpiatek.linkshorteneranalyticsservice.enricher;

record ParsedUserAgent(
        String deviceType,
        String os,
        String browser
) {}