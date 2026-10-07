package eu.openvalue.timesheet.domain;

import java.time.YearMonth;

public final class TimesheetNotSubmittedException extends DomainException {

  TimesheetNotSubmittedException(YearMonth month) {
    super("The timesheet for %s has not been submitted, so there is nothing to correct".formatted(month));
  }
}
