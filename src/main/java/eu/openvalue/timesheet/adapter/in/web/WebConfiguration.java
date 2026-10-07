package eu.openvalue.timesheet.adapter.in.web;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

  private final CurrentProfessional currentProfessional;

  WebConfiguration(CurrentProfessional currentProfessional) {
    this.currentProfessional = currentProfessional;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new CurrentProfessionalArgumentResolver(currentProfessional));
  }
}
