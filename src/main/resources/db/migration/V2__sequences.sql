-- =====================================================
-- V2 — Tabla auxiliar Sequences (generación atómica de IDs)
-- =====================================================
IF OBJECT_ID('Sequences', 'U') IS NULL
BEGIN
    CREATE TABLE Sequences (
        Name NVARCHAR(50) NOT NULL PRIMARY KEY,
        CurrentValue INT NOT NULL
    );

    INSERT INTO Sequences (Name, CurrentValue) VALUES (N'UsuarioId', 0);
END;
