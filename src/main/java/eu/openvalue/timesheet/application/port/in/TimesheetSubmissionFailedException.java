package eu.openvalue.timesheet.application.port.in;

import java.time.YearMonth;

public final class TimesheetSubmissionFailedException extends RuntimeException {

  public TimesheetSubmissionFailedException(YearMonth month, Throwable cause) {
    super("The timesheet for %s could not be handed in".formatted(month), cause);
  }
}
