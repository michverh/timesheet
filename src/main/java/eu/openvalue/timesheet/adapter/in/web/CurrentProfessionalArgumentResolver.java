package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Professional;
import java.util.Objects;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Gives handler methods with a {@link Professional} parameter the professional using the application, so that
 * controllers do not need to know where that comes from.
 */
final class CurrentProfessionalArgumentResolver implements HandlerMethodArgumentResolver {

  private final CurrentProfessional currentProfessional;

  CurrentProfessionalArgumentResolver(CurrentProfessional currentProfessional) {
    this.currentProfessional = Objects.requireNonNull(currentProfessional, "currentProfessional");
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(Professional.class);
  }

  @Override
  public Professional resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    return currentProfessional.get();
  }
}
