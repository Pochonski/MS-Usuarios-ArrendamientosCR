-- =====================================================
-- MS-Usuarios - Plataforma Arrendamientos CR
-- Migración V4: agregar columna GitHubId para login con GitHub OAuth
-- (separada de V5 para que SQL Server compile cada batch con la
--  columna ya presente — el parser valida refs antes de ejecutar)
-- =====================================================

ALTER TABLE Usuarios ADD GitHubId BIGINT NULL;
