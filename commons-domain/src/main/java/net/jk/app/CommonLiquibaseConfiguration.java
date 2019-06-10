package net.jk.app;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import net.jk.app.commons.boot.jpa.utils.LiquibaseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Custom Spring configuration to handle having 2 separate Liquibase instances in the app (TENANT +
 * SYSTEM)
 */
@Configuration
public class CommonLiquibaseConfiguration {

  @Autowired private ResourceLoader resourceLoader;

  @Autowired
  @Qualifier("masterDataSource") // tenant MASTER datasource, the default in the app
  private DataSource dataSource;

  @Autowired
  @Qualifier("masterSystemDataSource") // system MASTER datasource
  private DataSource systemDataSource;

  /** Main Liquibase config for TENANT database */
  @Bean("liquibaseProperties")
  @ConfigurationProperties("tenant.liquibase")
  public LiquibaseProperties liquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean("liquibase")
  public SpringLiquibase liquibase() {
    return LiquibaseUtils.createSpringLiquibase(liquibaseProperties(), dataSource, resourceLoader);
  }

  /** Secondary Liquibase config for SYSTEM database */
  @Bean("systemLiquibaseProperties")
  @ConfigurationProperties("system.liquibase")
  public LiquibaseProperties systemLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean("systemLiquibase")
  public SpringLiquibase systemLiquibase() {
    return LiquibaseUtils.createSpringLiquibase(
        systemLiquibaseProperties(), systemDataSource, resourceLoader);
  }
}
