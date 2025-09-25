package pl.bpiatek.linkshorteneranalyticsservice.click;

import java.time.Instant;

record EnrichedClick(
        Long id,
        // --- Core Identifiers ---
        String clickId,      // The unique ID for this click event (e.g., a deterministic UUID)
        String linkId,       // The ID of the link entity
        String userId,       // The ID of the user who owns the link
        String shortUrl,     // The short URL that was clicked

        // --- Raw Data (for auditing) ---
        Instant clickedAt,   // The timestamp of the click
        String ipAddress,
        String userAgent,

        // --- GeoIP Enrichment ---
        String countryCode,
        String cityName,
        String asn,

        // --- User-Agent Enrichment ---
        String deviceType,
        String osName,
        String browserName
) {
    EnrichedClick withId(Long id) {
        return new EnrichedClick(id, clickId, linkId, userId, shortUrl, clickedAt, ipAddress, userAgent, countryCode, cityName, asn, deviceType, osName, browserName);
    }
}
