package pl.bpiatek.linkshorteneranalyticsservice.enricher;

record EnrichedGeoInfo(
        String city,
        String country,
        String asnOrganization
) {}
