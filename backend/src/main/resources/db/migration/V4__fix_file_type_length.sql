-- file_type was VARCHAR(50), too short for MIME types like
-- application/vnd.openxmlformats-officedocument.wordprocessingml.document (71 chars)
ALTER TABLE lesson_files ALTER COLUMN file_type TYPE VARCHAR(255);
