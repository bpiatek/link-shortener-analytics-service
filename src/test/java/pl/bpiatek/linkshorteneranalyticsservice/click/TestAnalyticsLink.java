package pl.bpiatek.linkshorteneranalyticsservice.click;

public class TestAnalyticsLink {
    private final String shortUrl;
    private final String linkId;
    private final String userId;
    private final boolean isActive;

    private TestAnalyticsLink(TestAnalyticsLinkBuilder builder) {
        this.shortUrl = builder.shortUrl;
        this.linkId = builder.linkId;
        this.userId = builder.userId;
        this.isActive = builder.isActive;
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

    public static class TestAnalyticsLinkBuilder {
        private String shortUrl = "en78Se";
        private String linkId = "12";
        private String userId = "user-13";
        private boolean isActive;

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

        public TestAnalyticsLink build() {
            return new TestAnalyticsLink(this);
        }

    }
}