package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Professional;
import java.util.Objects;

/**
 * The professional using the application. Stands in for authentication until users log in.
 */
public final class CurrentProfessional {

  private final Professional professional;

  public CurrentProfessional(Professional professional) {
    this.professional = Objects.requireNonNull(professional, "professional");
  }

  public static CurrentProfessional from(CurrentProfessionalProperties properties) {
    return new CurrentProfessional(Professional.withEmail(properties.email()));
  }

  Professional get() {
    return professional;
  }
}
