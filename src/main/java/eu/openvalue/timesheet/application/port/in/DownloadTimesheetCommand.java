package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import java.util.Objects;

public record DownloadTimesheetCommand(Professional professional, YearMonth month) {

  public DownloadTimesheetCommand {
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(month, "month");
  }
}
