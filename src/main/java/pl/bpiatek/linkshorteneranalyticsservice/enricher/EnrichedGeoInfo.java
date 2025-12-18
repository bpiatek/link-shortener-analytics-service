package pl.bpiatek.linkshorteneranalyticsservice.enricher;

record EnrichedGeoInfo(
        String city,
        String cityLatitude,
        String cityLongitude,
        String country,
        String asnOrganization
) {}
