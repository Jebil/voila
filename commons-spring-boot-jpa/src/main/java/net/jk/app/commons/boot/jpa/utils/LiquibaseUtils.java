package net.jk.app.commons.boot.jpa.utils;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import net.jk.app.commons.boot.exception.ServerRuntimeException;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/** Common liquibase utilities */
public class LiquibaseUtils {

  /**
   * Common logic to create a SpringLiquibase bean from a set of Liquibase properties and assign it
   * to a datasource
   */
  public static SpringLiquibase createSpringLiquibase(
      LiquibaseProperties properties, DataSource dataSource, ResourceLoader resourceLoader) {
    // Locate change log file
    String changelogFile = properties.getChangeLog();
    Resource resource = resourceLoader.getResource(changelogFile);

    if (resource == null) {
      throw new ServerRuntimeException("Unable to find file");
    }

    // Configure Liquibase
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setChangeLog(changelogFile);
    liquibase.setDataSource(dataSource);
    liquibase.setShouldRun(true);

    return liquibase;
  }
}
