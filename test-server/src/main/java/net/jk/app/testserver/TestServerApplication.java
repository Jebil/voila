package net.jk.app.testserver;

import net.jk.app.CommonDomainConfiguration;
import net.jk.app.commons.boot.VoilaCommonsSpringBootConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableDiscoveryClient
@EnableSwagger2
@Import({VoilaCommonsSpringBootConfiguration.class, CommonDomainConfiguration.class})
public class TestServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(TestServerApplication.class, args);
  }
}
