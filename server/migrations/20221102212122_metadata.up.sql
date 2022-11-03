CREATE INDEX motifs_isrc_idx ON motifs (isrc);

CREATE TABLE isrc_metadata_status
(
    isrc       VARCHAR(12)              NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (isrc)
);

CREATE TABLE isrc_metadata
(
    isrc          VARCHAR(12) NOT NULL,
    name          VARCHAR     NOT NULL,
    artist        VARCHAR     NOT NULL,
    cover_art_url VARCHAR,
    PRIMARY KEY (isrc),
    FOREIGN KEY (isrc) REFERENCES isrc_metadata_status (isrc)
);

CREATE TABLE isrc_services
(
    id         SERIAL      NOT NULL, -- Can't use (isrc, service) composite bc/o lacking PG type PK support from SeaORM
    isrc       VARCHAR(12) NOT NULL,
    service    service     NOT NULL,
    service_id VARCHAR     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (isrc, service)
);