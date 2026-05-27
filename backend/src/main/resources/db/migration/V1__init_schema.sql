-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Groups
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    teacher_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Group members
CREATE TABLE group_members (
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (group_id, user_id)
);

-- Lessons
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    teacher_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Lesson access (which groups can access a lesson)
CREATE TABLE lesson_access (
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (lesson_id, group_id)
);

-- Lesson files
CREATE TABLE lesson_files (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    original_name VARCHAR(500) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    is_image BOOLEAN NOT NULL DEFAULT FALSE,
    is_video BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Words (vocabulary entries)
CREATE TABLE words (
    id BIGSERIAL PRIMARY KEY,
    english_word VARCHAR(500) NOT NULL,
    russian_translation VARCHAR(1000) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    lesson_id BIGINT REFERENCES lessons(id) ON DELETE SET NULL,
    group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Prevent duplicate words per user (same word from same source)
CREATE UNIQUE INDEX idx_words_unique
    ON words(user_id, english_word, source_type, COALESCE(lesson_id, 0), COALESCE(group_id, 0));

-- Messages (both personal and group chat)
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT check_message_target CHECK (
        (receiver_id IS NOT NULL AND group_id IS NULL) OR
        (receiver_id IS NULL AND group_id IS NOT NULL)
    )
);

-- Test sessions
CREATE TABLE test_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL,
    total_count INT NOT NULL,
    correct_count INT NOT NULL DEFAULT 0,
    score_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    direction VARCHAR(20) NOT NULL DEFAULT 'EN_TO_RU',
    word_filter_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    filter_lesson_id BIGINT REFERENCES lessons(id) ON DELETE SET NULL,
    filter_group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Test session word results
CREATE TABLE test_results (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES test_sessions(id) ON DELETE CASCADE,
    word_id BIGINT REFERENCES words(id) ON DELETE SET NULL,
    english_word VARCHAR(500) NOT NULL,
    russian_translation VARCHAR(1000) NOT NULL,
    user_answer VARCHAR(500),
    is_correct BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes
CREATE INDEX idx_words_user_id ON words(user_id);
CREATE INDEX idx_words_source_type ON words(source_type);
CREATE INDEX idx_words_lesson_id ON words(lesson_id);
CREATE INDEX idx_words_group_id ON words(group_id);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_receiver ON messages(receiver_id);
CREATE INDEX idx_messages_group ON messages(group_id);
CREATE INDEX idx_messages_created ON messages(created_at DESC);
CREATE INDEX idx_test_sessions_user ON test_sessions(user_id);
CREATE INDEX idx_test_sessions_group ON test_sessions(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);
CREATE INDEX idx_lesson_access_group ON lesson_access(group_id);
