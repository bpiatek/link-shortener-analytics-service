package pl.bpiatek.linkshorteneranalyticsservice.click;

record EnrichedData(
        String countryCode,
        String cityName,
        String asn,
        String deviceType,
        String osName,
        String browserName
) {}