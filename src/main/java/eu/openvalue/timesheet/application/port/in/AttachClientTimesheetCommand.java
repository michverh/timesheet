package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import java.util.Objects;

public record AttachClientTimesheetCommand(Professional professional, YearMonth month,
    ClientTimesheetDocument document) {

  public AttachClientTimesheetCommand {
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(document, "document");
  }
}
