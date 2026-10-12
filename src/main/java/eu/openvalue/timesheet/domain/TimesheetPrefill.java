package eu.openvalue.timesheet.domain;

import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

/**
 * Drafts a timesheet from the standard work week of 8 hours on Monday to Friday.
 *
 * <ul>
 *   <li>Weekends stay empty.</li>
 *   <li>A public holiday is booked as such.</li>
 *   <li>Other weekdays are booked as available when the professional is on the bench. When the professional works for
 *   a client, they stay empty: the hours worked come from the client's timesheets.</li>
 * </ul>
 */
public final class TimesheetPrefill {

  private final DutchPublicHolidays publicHolidays;

  public TimesheetPrefill(DutchPublicHolidays publicHolidays) {
    this.publicHolidays = Objects.requireNonNull(publicHolidays, "publicHolidays");
  }

  public Timesheet prefill(YearMonth month, Professional professional, Optional<Assignment> assignment) {
    var timesheet = Timesheet.start(month, professional, assignment);
    timesheet.days().stream()
        .filter(day -> !day.isWeekend())
        .map(DayEntry::date)
        .forEach(date -> {
          if (publicHolidays.isPublicHoliday(date)) {
            timesheet.bookHours(date, HourCategory.PUBLIC_HOLIDAY, DayEntry.STANDARD_WORKDAY);
          } else if (timesheet.isOnBench()) {
            timesheet.bookHours(date, HourCategory.AVAILABLE, DayEntry.STANDARD_WORKDAY);
          }
        });
    return timesheet;
  }
}
