package net.jk.app.commons.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Helper class to allow access to key Spring beans from static non-Spring classes */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

  // keep this private, we do NOT want to give access to entire Spring context from anywhere
  // we want to limit static access to only key beans that are really useful in non-Spring classes
  // it should a VERY small set of beans
  private static ApplicationContext context;

  /** Internationalization source. Used in all error messages */
  @Getter private static MessageSource i18n;

  /** Common Jackson mapper * */
  @Getter private static ObjectMapper objectMapper;

  @SuppressFBWarnings("ST")
  @Override
  public void setApplicationContext(ApplicationContext ac) throws BeansException {
    context = ac;
    this.i18n = ac.getBean(MessageSource.class);
    this.objectMapper = ac.getBean(ObjectMapper.class);
  }
}
