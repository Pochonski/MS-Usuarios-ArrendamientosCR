-- =====================================================
-- MS-Usuarios - Plataforma Arrendamientos CR
-- Database Schema for Azure SQL Database (SQL Server)
-- Migración inicial (V1) — replica sql/schema.azure.sql
-- =====================================================

-- TABLA: USUARIOS
IF OBJECT_ID('Usuarios', 'U') IS NULL
BEGIN
    CREATE TABLE Usuarios (
        Id NVARCHAR(50) NOT NULL PRIMARY KEY,
        Nombre NVARCHAR(100) NOT NULL,
        Correo NVARCHAR(255) NOT NULL,
        ContrasenaHash NVARCHAR(MAX) NULL,
        Rol NVARCHAR(20) NOT NULL,
        Telefono NVARCHAR(20) NULL,
        Avatar NVARCHAR(500) NULL,
        GoogleId NVARCHAR(255) NULL,
        FechaRegistro DATETIME2 NOT NULL DEFAULT GETDATE(),
        UltimoLogin DATETIME2 NULL,
        CONSTRAINT CK_Usuarios_Rol CHECK (Rol IN ('dueno', 'inquilino'))
    );

    CREATE UNIQUE INDEX IX_Usuarios_Correo ON Usuarios (Correo);
    CREATE INDEX IX_Usuarios_GoogleId ON Usuarios (GoogleId);
END;
