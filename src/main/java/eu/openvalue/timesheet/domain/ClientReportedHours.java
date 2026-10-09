package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The hours worked per day as a client registered them, for the days its timesheet covers. A client's timesheet can
 * cover fewer days than a month, such as a single week, or days of a neighbouring month.
 */
public final class ClientReportedHours {

  private final SortedMap<LocalDate, Hours> hoursWorked;

  /**
   * @param hoursWorkedPerDay the hours worked on every day the client's timesheet covers, zero when none
   */
  public ClientReportedHours(Map<LocalDate, Hours> hoursWorkedPerDay) {
    this.hoursWorked = Collections.unmodifiableSortedMap(new TreeMap<>(Objects.requireNonNull(hoursWorkedPerDay,
        "hoursWorkedPerDay")));
  }

  /**
   * @return the hours worked as the client registered them, or empty when the client's timesheet does not cover the
   *     day
   */
  public Optional<Hours> on(LocalDate date) {
    return Optional.ofNullable(hoursWorked.get(date));
  }

  /**
   * The hours worked on all days the client's timesheet covers, including days outside the month.
   */
  public Hours total() {
    return hoursWorked.values().stream().reduce(Hours.ZERO, Hours::plus);
  }

  /**
   * The hours worked on every day the client's timesheet covers.
   */
  public SortedMap<LocalDate, Hours> hoursWorkedPerDay() {
    return Collections.unmodifiableSortedMap(hoursWorked);
  }

  /**
   * The first day the client's timesheet covers, or empty when it covers no days.
   */
  public Optional<LocalDate> firstDay() {
    return hoursWorked.isEmpty() ? Optional.empty() : Optional.of(hoursWorked.firstKey());
  }

  /**
   * The last day the client's timesheet covers, or empty when it covers no days.
   */
  public Optional<LocalDate> lastDay() {
    return hoursWorked.isEmpty() ? Optional.empty() : Optional.of(hoursWorked.lastKey());
  }

  public boolean coversAnyDayOf(YearMonth month) {
    return hoursWorked.keySet().stream().anyMatch(date -> YearMonth.from(date).equals(month));
  }

  public boolean isEmpty() {
    return hoursWorked.isEmpty();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ClientReportedHours reported && hoursWorked.equals(reported.hoursWorked);
  }

  @Override
  public int hashCode() {
    return hoursWorked.hashCode();
  }

  @Override
  public String toString() {
    return "ClientReportedHours" + hoursWorked;
  }
}
