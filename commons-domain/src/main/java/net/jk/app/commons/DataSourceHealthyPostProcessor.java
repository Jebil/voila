package net.jk.app.commons;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * A {@link BeanPostProcessor} that tests acquiring a connection to a {@link DataSource} bean before
 * releasing it to be consumed by other spring beans
 */
@Slf4j
@Component
public class DataSourceHealthyPostProcessor implements BeanPostProcessor {

  private static final int MAX_RETRIES = 30;
  private static final long WAIT_MS = 1000;

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof DataSource) {
      DataSource dataSource = (DataSource) bean;

      int retries = 0;
      boolean healthy = false;
      while (!healthy) {
        log.info(
            "Attempt {} of {} to connect to data source {}", retries + 1, MAX_RETRIES, beanName);
        try (Connection conn = dataSource.getConnection()) {
          log.info("Successfully connected to data source {}", beanName);
          healthy = true;
        } catch (SQLException ex) {
          log.warn(
              "Unable to acquire connection on data source {} due to {}",
              beanName,
              ex.getMessage());

          if (++retries > MAX_RETRIES) {
            log.error("Failed to connect to data source {} after {} attempts", bean, MAX_RETRIES);
          } else {
            try {
              Thread.sleep(WAIT_MS);
            } catch (InterruptedException ie) {
            }
          }
        }
      }
    }
    return bean;
  }
}
