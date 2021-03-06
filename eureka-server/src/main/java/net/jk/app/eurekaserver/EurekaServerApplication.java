package net.jk.app.eurekaserver;

import net.jk.app.commons.boot.VoilaCommonsSpringBootConfiguration;
import net.jk.app.eurekaserver.security.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@EnableSwagger2
@Import({VoilaCommonsSpringBootConfiguration.class, SecurityConfig.class})
@EnableEurekaServer
@EnableConfigServer
public class EurekaServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }
}
