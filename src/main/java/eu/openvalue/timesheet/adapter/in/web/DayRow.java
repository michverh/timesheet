package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.DayEntry;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * One editable row of the timesheet page.
 *
 * @param bookedAsReportedByClient whether the hours worked are booked as the client's timesheets report them
 * @param leaveSuggested whether the leave on the day was filled in for hours not worked, and is yet to be confirmed
 * @param workdayHours the hours filling the workday, when they add up to more or less than a standard workday in a
 *     week that does not add up either; otherwise empty
 * @param error why the last input for this day was rejected, or {@code null}
 */
public record DayRow(LocalDate date, String label, boolean weekend, Map<HourCategory, String> hours,
    String kilometres, String explanation, boolean lacksExplanation, boolean bookedAsReportedByClient,
    boolean leaveSuggested, String workdayHours, String error) {

  private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);

  static DayRow of(DayEntry day, boolean bookedAsReportedByClient, boolean inDeviatingWeek) {
    var hours = new EnumMap<HourCategory, String>(HourCategory.class);
    for (var category : HourCategory.values()) {
      hours.put(category, format(day.hours(category)));
    }
    return new DayRow(day.date(), LABEL.format(day.date()), day.isWeekend(), hours, format(day.kilometres()),
        day.explanation().map(Explanation::text).orElse(""), day.lacksRequiredExplanation(),
        bookedAsReportedByClient, day.isLeaveSuggested(),
        inDeviatingWeek && day.deviatesFromStandardWorkday() ? HoursFormat.format(day.workdayHours()) : "", null);
  }

  /**
   * Shows rejected input again, so it can be corrected.
   */
  static DayRow rejected(LocalDate date, DayForm input, String error) {
    var dayOfWeek = date.getDayOfWeek();
    var weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    return new DayRow(date, LABEL.format(date), weekend, input.hours(), input.kilometres(), input.explanation(),
        false, false, false, "", error);
  }

  public boolean deviatesFromStandardWorkday() {
    return !workdayHours.isEmpty();
  }

  public YearMonth month() {
    return YearMonth.from(date);
  }

  static String format(Hours hours) {
    return hours.isZero() ? "" : HoursFormat.format(hours);
  }

  static String format(Kilometres kilometres) {
    return kilometres.isZero() ? "" : Integer.toString(kilometres.value());
  }
}
