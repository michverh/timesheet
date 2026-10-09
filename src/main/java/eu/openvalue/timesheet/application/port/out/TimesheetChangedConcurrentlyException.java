package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;

/**
 * Another change to the timesheet was kept after it was found, so keeping this one would undo that change.
 */
public class TimesheetChangedConcurrentlyException extends RuntimeException {

  public TimesheetChangedConcurrentlyException(Professional professional, YearMonth month) {
    super("The timesheet of %s for %s was changed by someone else since it was found".formatted(
        professional.email(), month));
  }

  public TimesheetChangedConcurrentlyException(Professional professional, YearMonth month, Throwable cause) {
    this(professional, month);
    initCause(cause);
  }
}
