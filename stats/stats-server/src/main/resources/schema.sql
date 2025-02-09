CREATE TABLE IF NOT EXISTS endpoint_hit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app VARCHAR(255) NOT NULL,
    uri VARCHAR(512) NOT NULL,
    ip VARCHAR(45) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_endpoint_hit_uri ON endpoint_hit(uri);
CREATE INDEX IF NOT EXISTS idx_endpoint_hit_timestamp ON endpoint_hit(timestamp);