package pl.bpiatek.linkshorteneranalyticsservice.click;

import java.time.Instant;

record EnrichedClick(
        Long id,
        String clickId,
        String linkId,
        String userId,
        String shortUrl,

        Instant clickedAt,
        String ipAddress,
        String userAgent,

        String countryCode,
        String cityName,
        String asn,

        String deviceType,
        String osName,
        String browserName
) {
    EnrichedClick withId(Long id) {
        return new EnrichedClick(id, clickId, linkId, userId, shortUrl, clickedAt, ipAddress, userAgent, countryCode, cityName, asn, deviceType, osName, browserName);
    }
}
