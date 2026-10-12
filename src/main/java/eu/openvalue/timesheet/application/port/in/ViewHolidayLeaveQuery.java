package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Professional;
import java.time.Year;
import java.util.Objects;

public record ViewHolidayLeaveQuery(Professional professional, Year year) {

  public ViewHolidayLeaveQuery {
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(year, "year");
  }
}
