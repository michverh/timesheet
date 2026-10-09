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
 * @param workdayHours the hours filling the workday, when they add up to more or less than a standard workday;
 *     otherwise empty
 * @param error why the last input for this day was rejected, or {@code null}
 */
public record DayRow(LocalDate date, String label, boolean weekend, Map<HourCategory, String> hours,
    String kilometres, String explanation, boolean lacksExplanation, String workdayHours, String error) {

  private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);

  static DayRow of(DayEntry day) {
    var hours = new EnumMap<HourCategory, String>(HourCategory.class);
    for (var category : HourCategory.values()) {
      hours.put(category, format(day.hours(category)));
    }
    return new DayRow(day.date(), LABEL.format(day.date()), day.isWeekend(), hours, format(day.kilometres()),
        day.explanation().map(Explanation::text).orElse(""), day.lacksRequiredExplanation(),
        day.deviatesFromStandardWorkday() ? HoursFormat.format(day.workdayHours()) : "", null);
  }

  /**
   * Shows rejected input again, so it can be corrected.
   */
  static DayRow rejected(LocalDate date, DayForm input, String error) {
    var dayOfWeek = date.getDayOfWeek();
    var weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    return new DayRow(date, LABEL.format(date), weekend, input.hours(), input.kilometres(), input.explanation(),
        false, "", error);
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
