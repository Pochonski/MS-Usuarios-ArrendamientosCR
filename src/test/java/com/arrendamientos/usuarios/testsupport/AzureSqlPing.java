package com.arrendamientos.usuarios.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utilidad standalone para probar conectividad a Azure SQL.
 * Uso:
 *   mvn -B -ntp -q test-compile
 *   java -cp "$(cat /tmp/cp.txt):target/test-classes:target/classes" \
 *        com.arrendamientos.usuarios.testsupport.AzureSqlPing \
 *        "jdbc:sqlserver://...;user=...;password=...;encrypt=true;trustServerCertificate=true"
 */
public class AzureSqlPing {
    public static void main(String[] args) throws Exception {
        String jdbcUrl = args.length > 0 ? args[0] : System.getenv("JDBC_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            System.err.println("JDBC_URL requerido (env o arg)");
            System.exit(1);
        }
        System.out.println("Conectando a: " + jdbcUrl.replaceAll("password=[^;]+", "password=***"));
        try (Connection c = DriverManager.getConnection(jdbcUrl)) {
            System.out.println("✓ Conectado. Producto: " + c.getMetaData().getDatabaseProductName()
                    + " " + c.getMetaData().getDatabaseProductVersion());
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT DB_NAME() as db, SUSER_NAME() as user_name, @@VERSION as version")) {
                if (rs.next()) {
                    System.out.println("  DB:     " + rs.getString("db"));
                    System.out.println("  User:   " + rs.getString("user_name"));
                    System.out.println("  Version: " + rs.getString("version").split("\n")[0]);
                }
            }
            // Listar tablas
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_SCHEMA, TABLE_NAME")) {
                System.out.println("  Tablas:");
                while (rs.next()) {
                    System.out.println("    - " + rs.getString("TABLE_SCHEMA") + "." + rs.getString("TABLE_NAME"));
                }
            }
        }
    }
}
