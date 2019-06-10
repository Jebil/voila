package net.jk.app.commons.boot.jpa.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routing datasource that can switch between MASTER (for read/write) and REPLICA (for read-only) in
 * order to enable horizontal scalability on reads via read slaves/replicas
 */
@Slf4j
public class ReplicaAwareRoutingDataSource extends AbstractDataSource {

  private final DataSource masterSource;
  private final DataSource replicaSource;

  public ReplicaAwareRoutingDataSource(DataSource masterSource, DataSource replicaSource) {
    this.masterSource = masterSource;
    this.replicaSource = replicaSource;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return getDataSource().getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return getDataSource().getConnection(username, password);
  }

  private DataSource getDataSource() {
    if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
      log.debug("Using REPLICA data source");
      return replicaSource;
    } else {
      log.debug("Using MASTER data source");
      return masterSource;
    }
  }
}
