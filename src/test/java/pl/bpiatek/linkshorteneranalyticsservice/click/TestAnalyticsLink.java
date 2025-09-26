package pl.bpiatek.linkshorteneranalyticsservice.click;

import java.time.Instant;
import java.time.LocalDateTime;

public class TestAnalyticsLink {
    private final String shortUrl;
    private final String linkId;
    private final String userId;
    private final boolean isActive;
    private final Instant deletedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private TestAnalyticsLink(TestAnalyticsLinkBuilder builder) {
        this.shortUrl = builder.shortUrl;
        this.linkId = builder.linkId;
        this.userId = builder.userId;
        this.isActive = builder.isActive;
        this.deletedAt = builder.deletedAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static TestAnalyticsLinkBuilder builder() {
        return new TestAnalyticsLinkBuilder();
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public String getLinkId() {
        return linkId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static class TestAnalyticsLinkBuilder {
        private String shortUrl = "en78Se";
        private String linkId = "12";
        private String userId = "user-13";
        private boolean isActive;
        private Instant deletedAt;
        private Instant createdAt;
        private Instant updatedAt;


        public TestAnalyticsLinkBuilder withShortUrl(String shortUrl) {
            this.shortUrl = shortUrl;
            return this;
        }

        public TestAnalyticsLinkBuilder withLinkId(String linkId) {
            this.linkId = linkId;
            return this;
        }

        public TestAnalyticsLinkBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public TestAnalyticsLinkBuilder withIsActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public TestAnalyticsLinkBuilder withDeletedAt(Instant deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public TestAnalyticsLinkBuilder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TestAnalyticsLinkBuilder withUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public TestAnalyticsLink build() {
            return new TestAnalyticsLink(this);
        }

    }
}