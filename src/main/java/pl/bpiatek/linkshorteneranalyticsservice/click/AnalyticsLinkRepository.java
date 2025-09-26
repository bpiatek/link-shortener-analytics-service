package pl.bpiatek.linkshorteneranalyticsservice.click;

import java.time.Instant;
import java.util.Optional;

interface AnalyticsLinkRepository {

    Optional<AnalyticsLink> findByShortUrl(String shortUrl);

    void save(AnalyticsLink link);

    void markAsDeleted(String shortUrl, Instant deletedAt);

    void updateStatus(String shortUrl, boolean isActive);

}
