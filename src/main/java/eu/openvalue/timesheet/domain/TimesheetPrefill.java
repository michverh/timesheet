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
 *   <li>Other weekdays are booked as worked when the professional has an assignment, and as available when on the
 *   bench.</li>
 * </ul>
 */
public final class TimesheetPrefill {

  private final DutchPublicHolidays publicHolidays;

  public TimesheetPrefill(DutchPublicHolidays publicHolidays) {
    this.publicHolidays = Objects.requireNonNull(publicHolidays, "publicHolidays");
  }

  public Timesheet prefill(YearMonth month, Professional professional, Optional<Assignment> assignment) {
    var timesheet = Timesheet.start(month, professional, assignment);
    var regularCategory = timesheet.isOnBench() ? HourCategory.AVAILABLE : HourCategory.WORKED;
    timesheet.days().stream()
        .filter(day -> !day.isWeekend())
        .map(DayEntry::date)
        .forEach(date -> timesheet.bookHours(date,
            publicHolidays.isPublicHoliday(date) ? HourCategory.PUBLIC_HOLIDAY : regularCategory,
            DayEntry.STANDARD_WORKDAY));
    return timesheet;
  }
}
