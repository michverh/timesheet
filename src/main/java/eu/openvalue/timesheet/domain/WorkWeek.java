package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Objects;

/**
 * The days of a week, Monday to Sunday, that fall within a month. Together, the hours filling the workday on them
 * should add up to a standard workday for each weekday among them; how they spread over the days is free.
 */
public record WorkWeek(List<DayEntry> days) {

  public WorkWeek {
    days = List.copyOf(Objects.requireNonNull(days, "days"));
    if (days.isEmpty()) {
      throw new IllegalArgumentException("A work week has at least one day");
    }
  }

  /**
   * The number of the week in its year, as in ISO 8601.
   */
  public int number() {
    return firstDay().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
  }

  public LocalDate firstDay() {
    return days.getFirst().date();
  }

  public LocalDate lastDay() {
    return days.getLast().date();
  }

  /**
   * The hours filling the workday booked on the days of the week.
   */
  public Hours bookedHours() {
    return days.stream().map(DayEntry::workdayHours).reduce(Hours.ZERO, Hours::plus);
  }

  /**
   * A standard workday for each weekday of the week.
   */
  public Hours standardHours() {
    var weekdays = (int) days.stream().filter(day -> !day.isWeekend()).count();
    return Hours.ofMinutes(DayEntry.STANDARD_WORKDAY.inMinutes() * weekdays);
  }

  public boolean deviatesFromStandardWorkWeek() {
    return !bookedHours().equals(standardHours());
  }

  /**
   * The hours that are missing to fill the standard work week; none when it is filled.
   */
  Hours shortfall() {
    return standardHours().isGreaterThan(bookedHours()) ? standardHours().minus(bookedHours()) : Hours.ZERO;
  }
}
