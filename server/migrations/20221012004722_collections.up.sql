CREATE TABLE collections
(
    id          UUID                     NOT NULL DEFAULT gen_random_uuid(),
    title       VARCHAR                  NOT NULL,
    description VARCHAR,
    owner_id    UUID                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (owner_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE collection_motifs
(
    collection_id UUID    NOT NULL,
    motif_id      INTEGER NOT NULL,
    PRIMARY KEY (collection_id, motif_id),
    FOREIGN KEY (collection_id) REFERENCES collections (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (motif_id) REFERENCES motifs (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
)