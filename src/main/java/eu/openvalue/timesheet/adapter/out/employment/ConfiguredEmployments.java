package eu.openvalue.timesheet.adapter.out.employment;

import eu.openvalue.timesheet.application.port.out.Employments;
import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import java.util.Objects;

public final class ConfiguredEmployments implements Employments {

  private final LocalDate startDate;

  public ConfiguredEmployments(EmploymentProperties properties) {
    this.startDate = Objects.requireNonNull(properties.startDate(), "timesheet.employment.start-date");
  }

  @Override
  public LocalDate employedSince(Professional professional) {
    return startDate;
  }
}
