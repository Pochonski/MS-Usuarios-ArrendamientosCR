-- =====================================================
-- V3 — Phase 5: Account Lockout + Token Revocation
-- Replica sql/migrations/phase5-lockout.sql
-- =====================================================

-- 1. Campos de lockout a Usuarios
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Usuarios', 'U') AND name = 'IntentosFallidos')
BEGIN
    ALTER TABLE Usuarios ADD IntentosFallidos INT NOT NULL CONSTRAINT DF_Usuarios_IntentosFallidos DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Usuarios', 'U') AND name = 'BloqueadoHasta')
BEGIN
    ALTER TABLE Usuarios ADD BloqueadoHasta DATETIME2 NULL;
END;

-- 2. Tabla: TokensRevocados
IF OBJECT_ID('TokensRevocados', 'U') IS NULL
BEGIN
    CREATE TABLE TokensRevocados (
        TokenId NVARCHAR(255) NOT NULL,
        RevocadoEl DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        Expiracion DATETIME2 NULL,
        PRIMARY KEY (TokenId)
    );

    CREATE INDEX IX_TokensRevocados_Expiracion
        ON TokensRevocados (Expiracion)
        WHERE Expiracion IS NOT NULL;
END;
