CREATE TABLE comments
(
    id         INTEGER                  NOT NULL,
    motif_id   INTEGER                  NOT NULL,
    parent_id  INTEGER,
    "offset"   INTEGER,
    content    VARCHAR                  NOT NULL,
    author_id  UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (motif_id) REFERENCES motifs (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT chk_nested_no_offset CHECK ("offset" IS NULL OR parent_id IS NULL)
);

CREATE TABLE motif_likes
(
    motif_id INTEGER NOT NULL,
    liker_id UUID    NOT NULL,
    PRIMARY KEY (motif_id, liker_id),
    FOREIGN KEY (motif_id) REFERENCES motifs (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (liker_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE comment_likes
(
    comment_id INTEGER NOT NULL,
    liker_id   UUID    NOT NULL,
    PRIMARY KEY (comment_id, liker_id),
    FOREIGN KEY (comment_id) REFERENCES comments (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (liker_id) REFERENCES profiles (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);