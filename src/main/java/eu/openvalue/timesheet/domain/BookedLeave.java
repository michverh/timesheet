package eu.openvalue.timesheet.domain;

import java.time.YearMonth;
import java.util.Objects;

/**
 * The holiday leave booked in a professional's timesheet for a month.
 *
 * @param submitted whether the timesheet has been submitted, so the leave is taken rather than reserved
 */
public record BookedLeave(YearMonth month, Hours hours, boolean submitted) {

  public BookedLeave {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(hours, "hours");
  }
}
