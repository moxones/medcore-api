-- Índice para la búsqueda de refresh tokens por hash.
-- A partir del cambio a SHA-256 el token se busca con findByToken (igualdad
-- exacta), por lo que necesita índice para no escanear la tabla completa.
-- Ejecutar manualmente (el proyecto no usa Flyway).

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens (token);

-- Limpieza única de tokens viejos con hash BCrypt (ya no validan tras el
-- cambio a SHA-256; las sesiones existentes deben iniciar sesión de nuevo).
-- Los hashes BCrypt empiezan con '$2'.
DELETE FROM refresh_tokens WHERE token LIKE '$2%';
