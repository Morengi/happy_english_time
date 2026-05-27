-- Default admin user (password: Admin@123)
INSERT INTO users (full_name, nickname, email, password, role)
VALUES (
    'Администратор',
    'admin',
    'admin@english-platform.ru',
    '$2b$10$CG3XHynRIqRRHA950N2OSOAPp6T37wB2SnpTgFtNvio1MTZsyxW/m',
    'ADMIN'
) ON CONFLICT (nickname) DO NOTHING;
