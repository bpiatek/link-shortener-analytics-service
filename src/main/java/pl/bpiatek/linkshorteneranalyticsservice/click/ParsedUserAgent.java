package pl.bpiatek.linkshorteneranalyticsservice.click;

record ParsedUserAgent(
        String deviceType,
        String os,
        String browser
) {}