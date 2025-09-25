package pl.bpiatek.linkshorteneranalyticsservice.click;

import java.util.Optional;

interface AnalyticsLinkRepository {

    Optional<AnalyticsLink> findByShortUrl(String shortUrl);

    void save(AnalyticsLink link);

    void deactivate(String shortUrl);
}
