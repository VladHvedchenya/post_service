CREATE TABLE hashtag
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)                           NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE UNIQUE INDEX uq_hashtag_name ON hashtag (LOWER(name));

CREATE TABLE post_hashtag
(
    post_id    BIGINT NOT NULL,
    hashtag_id BIGINT NOT NULL,
    PRIMARY KEY (hashtag_id, post_id)
);

CREATE INDEX idx_post_hashtag_hashtag_id_post_id_desc ON post_hashtag (hashtag_id, post_id DESC);