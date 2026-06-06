DROP TABLE IF EXISTS chat_messages;

CREATE TABLE chat_rooms (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    branch_id  UUID NOT NULL REFERENCES branches(id),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (user_id, branch_id)
);

CREATE TABLE chat_messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id UUID    NOT NULL REFERENCES chat_rooms(id),
    sender_type  VARCHAR NOT NULL,
    sender_id    UUID    NOT NULL,
    content      TEXT    NOT NULL,
    is_read      BOOLEAN DEFAULT false,
    created_at   TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_chat_rooms_user_id    ON chat_rooms(user_id);
CREATE INDEX idx_chat_rooms_branch_id  ON chat_rooms(branch_id);
CREATE INDEX idx_chat_messages_room_id ON chat_messages(chat_room_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(chat_room_id, created_at);