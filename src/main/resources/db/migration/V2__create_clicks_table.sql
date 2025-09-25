CREATE TABLE clicks (
    id                  BIGSERIAL PRIMARY KEY,
    click_id            VARCHAR(255) NOT NULL UNIQUE,
    link_id             VARCHAR(255) NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    link_short_url      VARCHAR(255) NOT NULL,
    clicked_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    ip_address          VARCHAR(45),
    country_code        VARCHAR(10),
    city_name           VARCHAR(255),
    asn                 VARCHAR(255),
    user_agent          TEXT,
    device_type         VARCHAR(100),
    os_name             VARCHAR(100),
    browser_name        VARCHAR(100)
);


CREATE INDEX idx_clicks_on_link_id ON clicks (link_id);
CREATE INDEX idx_clicks_on_user_id ON clicks (user_id);
CREATE INDEX idx_clicks_on_clicked_at ON clicks (clicked_at);
CREATE INDEX idx_clicks_on_country_code ON clicks (country_code);