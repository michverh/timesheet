package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Drafts a timesheet from the standard work week of 8 hours on Monday to Friday. Weekends stay empty; on a weekday,
 * a public holiday takes precedence over an absence, which takes precedence over a regular working day. A regular
 * working day is booked as worked when the professional has an assignment, and as available when on the bench.
 */
public final class TimesheetPrefill {

  static final Hours STANDARD_WORKDAY = Hours.of(8);

  private final DutchPublicHolidays publicHolidays;

  public TimesheetPrefill(DutchPublicHolidays publicHolidays) {
    this.publicHolidays = Objects.requireNonNull(publicHolidays, "publicHolidays");
  }

  public Timesheet prefill(YearMonth month, Professional professional, Optional<Assignment> assignment,
      Collection<Absence> absences) {
    var knownAbsences = List.copyOf(absences);
    var timesheet = Timesheet.start(month, professional, assignment);
    timesheet.days().stream()
        .filter(day -> !day.isWeekend())
        .map(DayEntry::date)
        .forEach(date -> timesheet.bookHours(date, categoryOf(date, timesheet, knownAbsences), STANDARD_WORKDAY));
    return timesheet;
  }

  private HourCategory categoryOf(LocalDate weekday, Timesheet timesheet, List<Absence> absences) {
    if (publicHolidays.isPublicHoliday(weekday)) {
      return HourCategory.PUBLIC_HOLIDAY;
    }
    if (absences.stream().anyMatch(absence -> absence.covers(weekday))) {
      return HourCategory.LEAVE;
    }
    return timesheet.isOnBench() ? HourCategory.AVAILABLE : HourCategory.WORKED;
  }
}
