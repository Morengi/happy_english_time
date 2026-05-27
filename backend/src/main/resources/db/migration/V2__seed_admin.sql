-- Default admin user (password: Admin@123)
INSERT INTO users (full_name, nickname, email, password, role)
VALUES (
    'Администратор',
    'admin',
    'admin@english-platform.ru',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'ADMIN'
) ON CONFLICT (nickname) DO NOTHING;
