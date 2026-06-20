-- persons.first_name and persons.last_name were accidentally created as bytea.
-- Convert them to varchar so LOWER() and text search functions work correctly.
ALTER TABLE persons
    ALTER COLUMN first_name TYPE VARCHAR(255) USING convert_from(first_name, 'UTF8'),
    ALTER COLUMN last_name  TYPE VARCHAR(255) USING convert_from(last_name,  'UTF8');
