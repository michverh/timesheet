package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;

public final class TimesheetDraftNotFoundException extends RuntimeException {

  public TimesheetDraftNotFoundException(Professional professional, YearMonth month) {
    super("There is no draft timesheet of %s for %s".formatted(professional.email(), month));
  }
}
