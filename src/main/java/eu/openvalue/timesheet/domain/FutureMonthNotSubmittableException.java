package eu.openvalue.timesheet.domain;

import java.time.YearMonth;

public final class FutureMonthNotSubmittableException extends DomainException {

  FutureMonthNotSubmittableException(YearMonth month) {
    super("The timesheet for %s cannot be submitted before that month has started".formatted(month));
  }
}
