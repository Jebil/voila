package net.jk.app;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import net.jk.app.commons.boot.CommonConstants;
import net.jk.app.commons.boot.jpa.transaction.ReplicaAwareRoutingDataSource;
import net.jk.app.commons.domain.model.system.VoilaSystemEntity;
import net.jk.app.commons.domain.repository.system.VoilaSystemEntityRepository;
import net.jk.app.commons.security.domain.system.Tenant;
import net.jk.app.commons.security.repository.system.TenantRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** Spring Data JPA configuration for the SYSTEM DB */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "systemEntityManagerFactory",
    transactionManagerRef = CommonConstants.SYSTEM_TX_MANAGER,
    basePackageClasses = {TenantRepository.class, VoilaSystemEntityRepository.class})
public class SystemDatabaseConfiguration {

  @SuppressWarnings("rawtypes")
  private static final Class[] ENTITY_PACKAGES =
      new Class[] {Tenant.class, VoilaSystemEntity.class};

  @Bean(name = "masterSystemDataSource")
  @ConfigurationProperties(prefix = "system.datasource")
  public DataSource masterSystemDataSource() {
    return DataSourceBuilder.create().build();
  }

  /** READ-REPLICA SUPPORT */
  @Bean(name = "replicaSystemDataSource")
  @ConfigurationProperties(prefix = "system.replica")
  public DataSource replicaSystemDataSource() {
    return DataSourceBuilder.create().build();
  }

  /**
   * The default datasource that can switch dynamically between master & replica depending if
   * transaction is read/write or read-only
   */
  @Bean(name = "systemDataSource")
  public DataSource systemDataSource(
      @Qualifier("masterSystemDataSource") DataSource masterSystemDataSource,
      @Qualifier("replicaSystemDataSource") DataSource replicaSystemDataSource) {
    ReplicaAwareRoutingDataSource ds =
        new ReplicaAwareRoutingDataSource(masterSystemDataSource, replicaSystemDataSource);
    return new LazyConnectionDataSourceProxy(ds);
  }

  @Bean(name = "systemEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean systemEntityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("systemDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages(ENTITY_PACKAGES)
        .persistenceUnit("voila-system")
        .build();
  }

  @Bean(name = CommonConstants.SYSTEM_TX_MANAGER)
  public PlatformTransactionManager systemTransactionManager(
      @Qualifier("systemEntityManagerFactory") EntityManagerFactory barEntityManagerFactory) {
    return new JpaTransactionManager(barEntityManagerFactory);
  }

  @Bean(name = CommonConstants.SYSTEM_TX_TEMPLATE)
  public TransactionTemplate systemTransactionTemplate(
      @Qualifier(CommonConstants.SYSTEM_TX_MANAGER) PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }

  @Bean
  public DataSourceHealthIndicator systemDataSourceHealthIndicator(
      @Qualifier("masterSystemDataSource") DataSource dataSource) {
    return new DataSourceHealthIndicator(dataSource, "SELECT 1");
  }

  @Bean
  public DataSourceHealthIndicator replicaSystemDataSourceHealthIndicator(
      @Qualifier("replicaSystemDataSource") DataSource dataSource) {
    return new DataSourceHealthIndicator(dataSource, "SELECT 1");
  }
}
