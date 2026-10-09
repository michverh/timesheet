package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import java.util.Objects;

public record TakeOverClientHoursCommand(Professional professional, YearMonth month) {

  public TakeOverClientHoursCommand {
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(month, "month");
  }
}
