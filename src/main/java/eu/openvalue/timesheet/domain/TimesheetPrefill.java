package eu.openvalue.timesheet.domain;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.reducing;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Drafts a timesheet from the standard work week of 8 hours on Monday to Friday.
 *
 * <ul>
 *   <li>Weekends stay empty.</li>
 *   <li>A public holiday is booked as such, regardless of any registered absence.</li>
 *   <li>Absences are booked in the category of their type, with their explanation.</li>
 *   <li>The remainder of the standard workday is booked as worked when the professional has an assignment, and as
 *   available when on the bench.</li>
 * </ul>
 */
public final class TimesheetPrefill {


  private final DutchPublicHolidays publicHolidays;

  public TimesheetPrefill(DutchPublicHolidays publicHolidays) {
    this.publicHolidays = Objects.requireNonNull(publicHolidays, "publicHolidays");
  }

  public Timesheet prefill(YearMonth month, Professional professional, Optional<Assignment> assignment,
      Collection<Absence> absences) {
    var timesheet = Timesheet.start(month, professional, assignment);
    var absencesByDate = absences.stream().collect(groupingBy(Absence::date));
    timesheet.days().stream()
        .filter(day -> !day.isWeekend())
        .map(DayEntry::date)
        .forEach(date -> prefillWeekday(timesheet, date, absencesByDate.getOrDefault(date, List.of())));
    return timesheet;
  }

  /**
   * Drafts a timesheet when the absences HR registered cannot be known, as if there were none. The timesheet remembers
   * this, so leave and sickness can be checked by hand.
   */
  public Timesheet prefillWithoutRegisteredAbsences(YearMonth month, Professional professional,
      Optional<Assignment> assignment) {
    var timesheet = prefill(month, professional, assignment, List.of());
    timesheet.markRegisteredAbsencesUnknown();
    return timesheet;
  }

  private void prefillWeekday(Timesheet timesheet, LocalDate date, List<Absence> absences) {
    if (publicHolidays.isPublicHoliday(date)) {
      timesheet.bookHours(date, HourCategory.PUBLIC_HOLIDAY, DayEntry.STANDARD_WORKDAY);
      return;
    }
    absentHoursPerCategory(absences).forEach((category, hours) -> timesheet.bookHours(date, category, hours));
    explanationOf(absences).ifPresent(explanation -> timesheet.explain(date, explanation));

    var absentHours = timesheet.day(date).totalHours();
    if (DayEntry.STANDARD_WORKDAY.isGreaterThan(absentHours)) {
      var regularCategory = timesheet.isOnBench() ? HourCategory.AVAILABLE : HourCategory.WORKED;
      timesheet.bookHours(date, regularCategory, DayEntry.STANDARD_WORKDAY.minus(absentHours));
    }
  }

  private static Map<HourCategory, Hours> absentHoursPerCategory(List<Absence> absences) {
    return absences.stream().collect(
        groupingBy(absence -> absence.type().category(), reducing(Hours.ZERO, Absence::hours, Hours::plus)));
  }

  private static Optional<Explanation> explanationOf(List<Absence> absences) {
    var text = absences.stream()
        .flatMap(absence -> absence.explanation().stream())
        .map(Explanation::text)
        .distinct()
        .collect(joining("; "));
    return text.isEmpty() ? Optional.empty() : Optional.of(new Explanation(text));
  }
}
