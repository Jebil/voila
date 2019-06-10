package net.jk.app.commons;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import net.jk.app.commons.boot.exception.ServerRuntimeException;

/** Common utilities for the system database, mostly for testing support */
@SuppressFBWarnings("EXS")
public class SystemDatabaseUtils {

  private static final String TRUNCATE_SQL = "DELETE FROM tenant WHERE system = false; ";

  /** Truncatesd all non-system generated entries in SYSTEM DB for BDD support between scenarios */
  public static void truncateSystemDatabaseForTesting(Connection systemConnection) {
    try {
      systemConnection.setAutoCommit(false);
      systemConnection.prepareStatement(TRUNCATE_SQL).execute();
      systemConnection.commit();
    } catch (SQLException e) {
      try {
        systemConnection.rollback();
      } catch (SQLException e1) {
        throw new ServerRuntimeException("SQL error on truncating SYSTEM DB", e);
      }
      throw new ServerRuntimeException("SQL error on truncating SYSTEM DB", e);
    }
  }
}
