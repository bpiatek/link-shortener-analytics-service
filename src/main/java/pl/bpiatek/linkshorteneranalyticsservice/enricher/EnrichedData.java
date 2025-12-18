package pl.bpiatek.linkshorteneranalyticsservice.enricher;

public record EnrichedData(
        String countryCode,
        String cityName,
        String cityLatitude,
        String cityLongitude,
        String asn,
        String deviceType,
        String osName,
        String browserName
) {}