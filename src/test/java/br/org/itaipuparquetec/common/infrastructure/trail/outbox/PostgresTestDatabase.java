package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration.AuditOutboxMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;
import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresTestDatabase {
   private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

   private PostgresTestDatabase() {
   }

   public static String newSchemaName() {
      String var10000 = UUID.randomUUID().toString();
      return "audit_" + var10000.replace("-", "");
   }

   public static HikariDataSource dataSourceOf(String schema, int maximumPoolSize) {
      createSchema(schema);
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(POSTGRES.getJdbcUrl());
      config.setUsername(POSTGRES.getUsername());
      config.setPassword(POSTGRES.getPassword());
      config.setMaximumPoolSize(maximumPoolSize);
      config.setConnectionTimeout(3000L);
      config.setConnectionInitSql("SET search_path TO " + schema + ", public");
      return new HikariDataSource(config);
   }

   public static HikariDataSource dataSourceWithOutboxTable(String schema, int maximumPoolSize) {
      HikariDataSource migrationPool = dataSourceOf(schema, 2);

      try {
         (new AuditOutboxMigrator()).migrate(migrationPool, schema);
      } catch (Throwable var6) {
         if (migrationPool != null) {
            try {
               migrationPool.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (migrationPool != null) {
         migrationPool.close();
      }

      return dataSourceOf(schema, maximumPoolSize);
   }

   public static String jdbcUrl() {
      return POSTGRES.getJdbcUrl();
   }

   public static String username() {
      return POSTGRES.getUsername();
   }

   public static String password() {
      return POSTGRES.getPassword();
   }

   private static void createSchema(String schema) {
      try {
         Connection connection = POSTGRES.createConnection("");

         try {
            Statement statement = connection.createStatement();

            try {
               statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            } catch (Throwable var7) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable var8) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var5) {
                  var8.addSuppressed(var5);
               }
            }

            throw var8;
         }

         if (connection != null) {
            connection.close();
         }

      } catch (SQLException failure) {
         throw new IllegalStateException("Cannot create the test schema '" + schema + "'", failure);
      }
   }

   static {
      POSTGRES.start();
      Runtime var10000 = Runtime.getRuntime();
      PostgreSQLContainer var10003 = POSTGRES;
      Objects.requireNonNull(var10003);
      var10000.addShutdownHook(new Thread(var10003::stop));
   }
}
