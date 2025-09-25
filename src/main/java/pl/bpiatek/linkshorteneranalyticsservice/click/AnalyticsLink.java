package pl.bpiatek.linkshorteneranalyticsservice.click;

record AnalyticsLink(
        String shortUrl,
        String linkId,
        String userId,
        boolean isActive
) {}