package net.jk.app.commons.cucumber;

import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/** Common utilities for relational databases, including test support */
@Slf4j
@SuppressFBWarnings("EXS")
public class DatabaseTestUtils {

  private static final String GET_TABLES_SQL =
      "select tablename from pg_tables\n"
          + "WHERE schemaname = 'public'\n"
          + "AND tablename NOT IN ('databasechangeloglock','databasechangelog', 'permission', 'permission_group')";

  private static final ConcurrentMap<String, String> DB_TRUNCATION_SQL = new ConcurrentHashMap<>();

  /**
   * Connects by looking up ENV variables or fallsback to specified default values, if not found...
   */
  public static Connection connectToDb(
      EnvVariable jdbcUrl,
      EnvVariable user,
      EnvVariable password,
      int numberOfRetries,
      long pause) {

    return connectToDb(
        jdbcUrl.getValue(), user.getValue(), password.getValue(), numberOfRetries, pause);
  }

  public static Connection connectToDb(
      String jdbcUrl, String user, String password, int numberOfRetries, long pause) {

    // build full URL with embedded user/password
    String url =
        ThreadLocals.STRINGBUILDER
            .get()
            .append(jdbcUrl)
            .append("?user=")
            .append(user)
            .append("&password=")
            .append(password)
            .toString();

    Connection connection = null;
    int retries = 0;
    while (connection == null) {
      try {
        log.info("Attempting to connect to {} DB #{}", jdbcUrl, retries + 1);
        connection = DriverManager.getConnection(url);
        log.info("Successfully connected to {} DB", jdbcUrl);

        // clean up
        Connection shutdownDriver = connection;
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(
                    () -> {
                      log.info("Disconnecting from {} DB", jdbcUrl);
                      try {
                        shutdownDriver.close();
                      } catch (SQLException e) {
                        log.error("Connection close error", e);
                      }
                    }));

      } catch (Exception e) {
        log.warn("Unable to connect to {} DB due to {}", jdbcUrl, e.getClass().getSimpleName());
        retries++;
        if (retries > numberOfRetries) {
          log.error("Failed to connect to {} DB", jdbcUrl);
          throw new IllegalStateException(e.getMessage(), e);
        } else {
          try {
            Thread.sleep(pause);
          } catch (InterruptedException e1) {
          }
        }
      }
    }

    return connection;
  }

  /**
   * Truncates all tables in a PostgreSQL database
   *
   * @param keyTables list of key tables that should be truncated first for FKs
   */
  @SuppressFBWarnings("SECSQLIJDBC")
  public static void truncateAllTables(Connection connection, String dbName, String... keyTables) {
    try {
      Instant start = Instant.now();
      log.debug("Truncating {} DB", dbName);
      connection.prepareStatement(getTruncationSql(connection, dbName, keyTables)).execute();
      Instant end = Instant.now();

      log.debug(
          "Truncated {} DB in {} ms", dbName, end.minusMillis(start.toEpochMilli()).toEpochMilli());

    } catch (SQLException e) {
      throw new IllegalStateException("Truncation error for " + dbName, e);
    }
  }

  private static String getTruncationSql(Connection connection, String dbName, String... keyTables)
      throws SQLException {
    String sql = DB_TRUNCATION_SQL.get(dbName);
    if (sql == null) {

      StringBuilder sb = ThreadLocals.STRINGBUILDER.get();

      Arrays.stream(keyTables).forEach(t -> sb.append("DELETE FROM ").append(t).append("; "));
      Set<String> processed = Sets.newHashSet(keyTables);

      try (ResultSet rs = connection.prepareStatement(GET_TABLES_SQL).executeQuery()) {
        while (rs.next()) {
          String table = rs.getString(1);

          if (!processed.contains(table)) {
            // DELETE is a lot faster than TRUNCATE for our small tables in BDDs
            sb.append("DELETE FROM ").append(rs.getString(1)).append("; ");
          }
        }
      }
      // sb.append(" CASCADE ");
      sql = sb.toString();
      DB_TRUNCATION_SQL.putIfAbsent(dbName, sql);
    }

    return sql;
  }
}
