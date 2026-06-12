CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(30) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    rating          INT NOT NULL DEFAULT 1500,
    max_rating      INT NOT NULL DEFAULT 1500,
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_rating ON users (rating DESC);
