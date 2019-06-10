package net.jk.app.heimdallserver;

import net.jk.app.CommonDomainConfiguration;
import net.jk.app.commons.boot.VoilaCommonsSpringBootConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@EnableZuulProxy
@Import({VoilaCommonsSpringBootConfiguration.class, CommonDomainConfiguration.class})
@EnableSwagger2
@EnableDiscoveryClient
public class HeimdallServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(HeimdallServerApplication.class, args);
  }
}
