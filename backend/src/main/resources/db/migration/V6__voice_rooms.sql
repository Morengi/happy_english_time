CREATE TABLE voice_rooms (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    creator_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    max_participants INTEGER,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at         TIMESTAMP
);

CREATE TABLE voice_room_participants (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT NOT NULL REFERENCES voice_rooms(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    left_at     TIMESTAMP,
    UNIQUE (room_id, user_id)
);

CREATE INDEX idx_voice_rooms_status  ON voice_rooms(status);
CREATE INDEX idx_voice_rooms_creator ON voice_rooms(creator_id);
CREATE INDEX idx_vrp_room            ON voice_room_participants(room_id);
CREATE INDEX idx_vrp_user            ON voice_room_participants(user_id);
