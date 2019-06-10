package net.jk.app;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import net.jk.app.commons.boot.CommonConstants;
import net.jk.app.commons.boot.jpa.transaction.ReplicaAwareRoutingDataSource;
import net.jk.app.commons.domain.model.tenant.Contact;
import net.jk.app.commons.domain.repository.tenant.ContactRepository;
import net.jk.app.commons.security.domain.tenant.ApplicationUser;
import net.jk.app.commons.security.repository.tenant.ApplicationUserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring Data JPA configuration for the tenant DB This is the **DEFAULT** DB in Spring Data, so
 * that we can use @Transactional everywhere without having to specify a TX manager name explicitly
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "entityManagerFactory",
    basePackageClasses = {ApplicationUserRepository.class, ContactRepository.class})
public class TenantDatabaseConfiguration {

  @Value(value = "${tenant.datasource.jdbcUrl}")
  private String masterUrl;

  /** PRIMARY (MASTER) */
  @Bean(name = "masterDataSource")
  @ConfigurationProperties(prefix = "tenant.datasource")
  public DataSource masterDataSource() {
    return DataSourceBuilder.create().build();
  }

  /** READ REPLICA */
  @Bean(name = "replicaDataSource")
  @ConfigurationProperties(prefix = "tenant.replica")
  public DataSource replicaDataSource() {
    return DataSourceBuilder.create().build();
  }

  /**
   * The default datasource that can switch dynamically between master & replica depending if
   * transaction is read/write or read-only
   */
  @Primary
  @Bean(name = "dataSource")
  public DataSource dataSource(
      @Qualifier("masterDataSource") DataSource masterDataSource,
      @Qualifier("replicaDataSource") DataSource replicaDataSource) {
    ReplicaAwareRoutingDataSource ds =
        new ReplicaAwareRoutingDataSource(masterDataSource, replicaDataSource);
    return new LazyConnectionDataSourceProxy(ds);
  }

  @Primary
  @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages(ApplicationUser.class, Contact.class)
        .persistenceUnit("voila-tenant")
        .build();
  }

  @Primary
  @Bean(name = "transactionManager")
  public PlatformTransactionManager tenantTransactionManager(
      @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }

  @Bean(name = CommonConstants.TENANT_TX_TEMPLATE)
  public TransactionTemplate systemTransactionTemplate(
      @Qualifier(CommonConstants.TENANT_TX_MANAGER) PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }

  @Bean
  public DataSourceHealthIndicator masterDataSourceHealthIndicator(
      @Qualifier("masterDataSource") DataSource dataSource) {
    return new DataSourceHealthIndicator(dataSource, "SELECT 1");
  }

  @Bean
  public DataSourceHealthIndicator replicaDataSourceHealthIndicator(
      @Qualifier("replicaDataSource") DataSource dataSource) {
    return new DataSourceHealthIndicator(dataSource, "SELECT 1");
  }

  /** For custom hard-core SQL queries from read slaves */
  @Primary
  @Bean
  public NamedParameterJdbcTemplate replicaJdbcTemplate(
      @Qualifier("replicaDataSource") DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }
}
