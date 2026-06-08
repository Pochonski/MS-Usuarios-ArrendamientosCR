-- =====================================================
-- MS-Usuarios - Plataforma Arrendamientos CR
-- Migración V4: agregar columna GitHubId para login con GitHub OAuth
-- =====================================================

IF COL_LENGTH('Usuarios', 'GitHubId') IS NULL
BEGIN
    ALTER TABLE Usuarios ADD GitHubId BIGINT NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_Usuarios_GitHubId' AND object_id = OBJECT_ID('Usuarios')
)
BEGIN
    CREATE UNIQUE INDEX IX_Usuarios_GitHubId
        ON Usuarios (GitHubId)
        WHERE GitHubId IS NOT NULL;
END;
