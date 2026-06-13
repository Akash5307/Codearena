-- Rating engine: mark which rated contests have had ratings applied (idempotency),
-- and record each user's per-contest rating change for history.

ALTER TABLE contests ADD COLUMN ratings_applied BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE rating_changes (
    id          BIGSERIAL PRIMARY KEY,
    contest_id  BIGINT NOT NULL REFERENCES contests(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    old_rating  INT NOT NULL,
    new_rating  INT NOT NULL,
    delta       INT NOT NULL,
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_rating_changes_user ON rating_changes (user_id);
CREATE INDEX idx_rating_changes_contest ON rating_changes (contest_id);
