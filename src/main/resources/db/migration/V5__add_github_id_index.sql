-- =====================================================
-- Migración V5: índice único filtrado sobre GitHubId
-- (corre DESPUÉS de V4 para garantizar que la columna existe)
-- =====================================================

CREATE UNIQUE INDEX IX_Usuarios_GitHubId
    ON Usuarios (GitHubId)
    WHERE GitHubId IS NOT NULL;
