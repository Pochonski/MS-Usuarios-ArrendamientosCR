-- =====================================================
-- Seed: 5 usuarios demo (3 dueños + 2 inquilinos)
-- Fecha: 2026-06-08
-- Generado por: opencode (sesión de seeding manual)
-- Entorno destino: Azure SQL `arrendamientoscr` / DB `usuarios_db`
--
-- ADVERTENCIA: estos son usuarios de DEMO con contraseñas
-- predecibles (patrón Arrendamientos2026!0N). Rotar antes
-- de cualquier uso productivo real.
--
-- Idempotente: cada INSERT está protegido por
--   IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE Correo = ...)
-- Re-ejecutable sin duplicar correos. Los IDs que ya estén
-- en uso se saltean (no se reciclan).
-- =====================================================

SET XACT_ABORT ON;
SET NOCOUNT ON;

BEGIN TRY
    BEGIN TRAN;

    -- Bloquear la fila de la secuencia para evitar carrera con la app
    DECLARE @baseId INT;
    SELECT @baseId = CurrentValue + 1
      FROM Sequences WITH (UPDLOCK, HOLDLOCK, ROWLOCK)
      WHERE Name = N'UsuarioId';

    DECLARE @assignedId NVARCHAR(50);
    DECLARE @hash NVARCHAR(MAX);

    -- ============ USUARIO 1: María Rodríguez (dueno) ============
    IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE Correo = N'maria.rodriguez@arrendamientoscr.com')
    BEGIN
        SET @assignedId = N'usr-' + RIGHT('000' + CAST(@baseId AS VARCHAR(3)), 3);
        SET @hash = N'$2a$10$gMKClN9sE7Q0n5PQq0aReOAQMVUpaZnrmNvuku/dwdZG218xJAxbO';
        INSERT INTO Usuarios (Id, Nombre, Correo, ContrasenaHash, Rol, Telefono, FechaRegistro)
        VALUES (@assignedId,
                N'María Rodríguez Vargas',
                N'maria.rodriguez@arrendamientoscr.com',
                @hash,
                N'dueno',
                N'+506 8888-1001',
                SYSUTCDATETIME());
        PRINT N'Insertado ' + @assignedId + N' -> maria.rodriguez@arrendamientoscr.com';
        SET @baseId = @baseId + 1;
    END
    ELSE
        PRINT N'YA EXISTE maria.rodriguez@arrendamientoscr.com — se omite (no se reasigna Id).';

    -- ============ USUARIO 2: Carlos Méndez (dueno) ============
    IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE Correo = N'carlos.mendez@arrendamientoscr.com')
    BEGIN
        SET @assignedId = N'usr-' + RIGHT('000' + CAST(@baseId AS VARCHAR(3)), 3);
        SET @hash = N'$2a$10$u377ca8GdqtPmAOzue2GvOrqVzd/lCSDhKYOtKwvFCneWYvgntbRe';
        INSERT INTO Usuarios (Id, Nombre, Correo, ContrasenaHash, Rol, Telefono, FechaRegistro)
        VALUES (@assignedId,
                N'Carlos Méndez Solís',
                N'carlos.mendez@arrendamientoscr.com',
                @hash,
                N'dueno',
                N'+506 8888-1002',
                SYSUTCDATETIME());
        PRINT N'Insertado ' + @assignedId + N' -> carlos.mendez@arrendamientoscr.com';
        SET @baseId = @baseId + 1;
    END
    ELSE
        PRINT N'YA EXISTE carlos.mendez@arrendamientoscr.com — se omite (no se reasigna Id).';

    -- ============ USUARIO 3: Ana Lucía Pérez (dueno) ============
    IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE Correo = N'ana.perez@arrendamientoscr.com')
    BEGIN
        SET @assignedId = N'usr-' + RIGHT('000' + CAST(@baseId AS VARCHAR(3)), 3);
        SET @hash = N'$2a$10$vnq22oZDsRc8RP65xO0mNetI3ZXNzPELjVjZck9iQ0EaAhKjQSuba';
        INSERT INTO Usuarios (Id, Nombre, Correo, ContrasenaHash, Rol, Telefono, FechaRegistro)
        VALUES (@assignedId,
                N'Ana Lucía Pérez Brenes',
                N'ana.perez@arrendamientoscr.com',
                @hash,
                N'dueno',
                N'+506 8888-1003',
                SYSUTCDATETIME());
        PRINT N'Insertado ' + @assignedId + N' -> ana.perez@arrendamientoscr.com';
        SET @baseId = @baseId + 1;
    END
    ELSE
        PRINT N'YA EXISTE ana.perez@arrendamientoscr.com — se omite (no se reasigna Id).';

    -- ============ USUARIO 4: Jorge Vargas (inquilino) ============
    IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE Correo = N'jorge.vargas@arrendamientoscr.com')
    BEGIN
        SET @assignedId = N'usr-' + RIGHT('000' + CAST(@baseId AS VARCHAR(3)), 3);
        SET @hash = N'$2a$10$5ZOXFphJvaurGvhPYz86qeCOjJVdc8QjTM4pArcHHx1xZtf2a/0UC';
        INSERT INTO Usuarios (Id, Nombre, Correo, ContrasenaHash, Rol, Telefono, FechaRegistro)
        VALUES (@assignedId,
                N'Jorge Andrés Vargas Ulate',
                N'jorge.vargas@arrendamientoscr.com',
                @hash,
                N'inquilino',
                N'+506 8888-1004',
                SYSUTCDATETIME());
        PRINT N'Insertado ' + @assignedId + N' -> jorge.vargas@arrendamientoscr.com';
        SET @baseId = @baseId + 1;
    END
    ELSE
        PRINT N'YA EXISTE jorge.vargas@arrendamientoscr.com — se omite (no se reasigna Id).';

    -- ============ USUARIO 5: Daniela Soto (inquilino) ============
    IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE Correo = N'daniela.soto@arrendamientoscr.com')
    BEGIN
        SET @assignedId = N'usr-' + RIGHT('000' + CAST(@baseId AS VARCHAR(3)), 3);
        SET @hash = N'$2a$10$Jeedc2NgMtm6ZyYraxud9egPKZvhmJSE5mZnIm0F3r.43wHTYdIrO';
        INSERT INTO Usuarios (Id, Nombre, Correo, ContrasenaHash, Rol, Telefono, FechaRegistro)
        VALUES (@assignedId,
                N'Daniela Soto Camacho',
                N'daniela.soto@arrendamientoscr.com',
                @hash,
                N'inquilino',
                N'+506 8888-1005',
                SYSUTCDATETIME());
        PRINT N'Insertado ' + @assignedId + N' -> daniela.soto@arrendamientoscr.com';
        SET @baseId = @baseId + 1;
    END
    ELSE
        PRINT N'YA EXISTE daniela.soto@arrendamientoscr.com — se omite (no se reasigna Id).';

    -- Persistir el nuevo CurrentValue (idempotente: si nada se insertó,
    -- @baseId - 1 == valor previo, UPDATE no produce cambio efectivo).
    UPDATE Sequences SET CurrentValue = @baseId - 1 WHERE Name = N'UsuarioId';

    COMMIT;
    PRINT N'>>> Seed aplicado OK. Sequences.UsuarioId = ' + CAST((@baseId - 1) AS NVARCHAR(10));
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    DECLARE @msg NVARCHAR(2000) = ERROR_MESSAGE();
    RAISERROR(@msg, 16, 1);
END CATCH;
