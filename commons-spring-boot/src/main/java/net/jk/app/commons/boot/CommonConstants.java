package net.jk.app.commons.boot;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Common constants */
public class CommonConstants {

  public static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");

  // known data sources
  public static final String TENANT_DATASOURCE = "dataSource"; // the default, for read/write
  public static final String SYSTEM_DATASOURCE = "systemDataSource"; // for read/write

  // Known system transaction managers

  public static final String TENANT_TX_MANAGER = "transactionManager"; // the default
  public static final String SYSTEM_TX_MANAGER = "systemTransactionManager"; // for read/writes

  public static final String TENANT_TX_TEMPLATE = "transactionTemplate"; // the default
  public static final String SYSTEM_TX_TEMPLATE = "systemTransactionTemplate"; // for read/writes

  public static final DateTimeFormatter OFFSETDATETIME_FORMATTER =
      DateTimeFormatter.ISO_OFFSET_DATE_TIME;
}
