CREATE TABLE analytics_links (
     short_url       VARCHAR(255) PRIMARY KEY,
     link_id         VARCHAR(255) NOT NULL,
     user_id         VARCHAR(255) NOT NULL,
     is_active       BOOLEAN NOT NULL DEFAULT true,
     deleted_at      TIMESTAMP WITH TIME ZONE,
     created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
     updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);