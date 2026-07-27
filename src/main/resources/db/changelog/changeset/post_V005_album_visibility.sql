ALTER TABLE album
    ADD COLUMN visibility VARCHAR(32) NOT NULL DEFAULT 'ALL_USERS';

CREATE TABLE album_allowed_users
(
    album_id BIGINT NOT NULL,
    user_id  BIGINT NOT NULL,
    CONSTRAINT pk_album_allowed_users PRIMARY KEY (album_id, user_id),
    CONSTRAINT fk_allowed_users_album FOREIGN KEY (album_id) REFERENCES album (id) ON DELETE CASCADE
);

CREATE INDEX idx_album_allowed_users_user ON album_allowed_users (user_id);
