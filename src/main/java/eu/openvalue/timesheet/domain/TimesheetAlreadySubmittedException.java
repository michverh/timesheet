package eu.openvalue.timesheet.domain;

import java.time.YearMonth;

public final class TimesheetAlreadySubmittedException extends DomainException {

  TimesheetAlreadySubmittedException(YearMonth month) {
    super("The timesheet for %s has already been submitted".formatted(month));
  }
}
