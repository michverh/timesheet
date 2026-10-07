package eu.openvalue.timesheet.domain;

import java.time.LocalDate;

public final class DailyHoursLimitExceededException extends DomainException {

  DailyHoursLimitExceededException(LocalDate date, Hours limit) {
    super("No more than %d hours can be booked on %s".formatted(limit.wholeHours(), date));
  }
}
