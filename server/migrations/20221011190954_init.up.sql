CREATE TABLE users
(
    id         UUID                     NOT NULL DEFAULT gen_random_uuid(),
    email      VARCHAR                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (email)
);
CREATE TYPE service AS ENUM ('SPOTIFY', 'APPLE_MUSIC');
CREATE TABLE service_credentials
(
    user_id               uuid    NOT NULL,
    service               service NOT NULL,
    service_id            VARCHAR NOT NULL,
    access_token          VARCHAR NOT NULL,
    access_token_expires  TIMESTAMP WITH TIME ZONE,
    refresh_token         VARCHAR NOT NULL,
    refresh_token_expires TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
CREATE TABLE refresh_tokens
(
    id         SERIAL                   NOT NULL,
    user_id    UUID                     NOT NULL,
    value      VARCHAR                  NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
CREATE TABLE profiles
(
    user_id      UUID    NOT NULL,
    username     VARCHAR NOT NULL,
    biography    VARCHAR,
    photo_url    VARCHAR,
    display_name VARCHAR NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE (username),
    FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
CREATE TABLE profile_follows
(
    follower_id UUID NOT NULL,
    followed_id UUID NOT NULL,
    PRIMARY KEY (follower_id, followed_id),
    FOREIGN KEY (follower_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (followed_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
CREATE TABLE motifs
(
    id         SERIAL                   NOT NULL,
    isrc       VARCHAR(12)              NOT NULL,
    "offset"   INTEGER                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    creator_id UUID                     NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (creator_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
CREATE TABLE motif_service_ids
(
    id         SERIAL  NOT NULL, -- Can't use (motif_id, service) composite bc/o lacking PG type PK support from SeaORM
    motif_id   INTEGER NOT NULL,
    service    service NOT NULL,
    service_id VARCHAR NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (motif_id) REFERENCES motifs (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    UNIQUE (motif_id, service)
);
CREATE TABLE motif_listeners
(
    motif_id    INTEGER                  NOT NULL,
    listener_id UUID                     NOT NULL,
    listened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (motif_id, listener_id),
    FOREIGN KEY (motif_id) REFERENCES motifs (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (listener_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
CREATE INDEX motif_listeners_order ON motif_listeners (motif_id, listened_at);

