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
import java.util.Optional;

/**
 * One editable row of the timesheet page.
 *
 * @param clientHours the hours worked according to the client's timesheet; empty when it does not cover the day
 * @param differsFromClient whether the hours booked as worked differ from the client's timesheet
 * @param error why the last input for this day was rejected, or {@code null}
 */
public record DayRow(LocalDate date, String label, boolean weekend, Map<HourCategory, String> hours,
    String kilometres, String explanation, boolean lacksExplanation, String clientHours, boolean differsFromClient,
    String error) {

  private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);

  /**
   * @param clientHours the hours worked according to the client's timesheet, if it covers the day
   * @param differsFromClient whether the hours booked as worked differ from the client's timesheet
   */
  static DayRow of(DayEntry day, Optional<Hours> clientHours, boolean differsFromClient) {
    var hours = new EnumMap<HourCategory, String>(HourCategory.class);
    for (var category : HourCategory.values()) {
      hours.put(category, format(day.hours(category)));
    }
    return new DayRow(day.date(), LABEL.format(day.date()), day.isWeekend(), hours, format(day.kilometres()),
        day.explanation().map(Explanation::text).orElse(""), day.lacksRequiredExplanation(),
        clientHours.map(HoursFormat::format).orElse(""), differsFromClient, null);
  }

  /**
   * Shows rejected input again, so it can be corrected.
   */
  static DayRow rejected(LocalDate date, DayForm input, String error) {
    var dayOfWeek = date.getDayOfWeek();
    var weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    return new DayRow(date, LABEL.format(date), weekend, input.hours(), input.kilometres(), input.explanation(),
        false, "", false, error);
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
