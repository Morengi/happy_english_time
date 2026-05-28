-- Fix admin password to Admin@123
UPDATE users SET password = '$2b$10$CG3XHynRIqRRHA950N2OSOAPp6T37wB2SnpTgFtNvio1MTZsyxW/m'
WHERE nickname = 'admin';
