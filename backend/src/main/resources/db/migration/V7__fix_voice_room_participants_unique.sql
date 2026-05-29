-- Drop the full unique constraint that prevents users from ever rejoining a room
-- after they have left (because the old left record still exists with (room_id, user_id)).
ALTER TABLE voice_room_participants
    DROP CONSTRAINT IF EXISTS voice_room_participants_room_id_user_id_key;

-- Replace with a PARTIAL unique index so only ONE active (left_at IS NULL) entry
-- per (room_id, user_id) is enforced.  Users can re-join after leaving.
CREATE UNIQUE INDEX IF NOT EXISTS idx_vrp_active
    ON voice_room_participants(room_id, user_id)
    WHERE left_at IS NULL;
