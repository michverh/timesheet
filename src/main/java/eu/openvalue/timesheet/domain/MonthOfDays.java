package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The entries of a timesheet: exactly one for every day of its month.
 */
final class MonthOfDays {

  private final YearMonth month;
  private final SortedMap<LocalDate, DayEntry> days;

  private MonthOfDays(YearMonth month, SortedMap<LocalDate, DayEntry> days) {
    this.month = month;
    this.days = Collections.unmodifiableSortedMap(days);
  }

  static MonthOfDays emptyFor(YearMonth month) {
    Objects.requireNonNull(month, "month");
    var days = new TreeMap<LocalDate, DayEntry>();
    month.atDay(1).datesUntil(month.atEndOfMonth().plusDays(1)).forEach(date -> days.put(date, DayEntry.empty(date)));
    return new MonthOfDays(month, days);
  }

  /**
   * @throws IllegalArgumentException when the entries are not exactly one for every day of the month
   */
  static MonthOfDays of(YearMonth month, List<DayEntry> entries) {
    var days = emptyFor(month);
    var dates = new HashSet<LocalDate>();
    for (var entry : entries) {
      if (!dates.add(entry.date())) {
        throw new IllegalArgumentException("%s has more than one entry".formatted(entry.date()));
      }
      days = days.with(entry);
    }
    if (dates.size() != month.lengthOfMonth()) {
      throw new IllegalArgumentException("%s needs an entry for each of its %d days, not %d".formatted(
          month, month.lengthOfMonth(), dates.size()));
    }
    return days;
  }

  List<DayEntry> all() {
    return List.copyOf(days.values());
  }

  /**
   * @throws IllegalArgumentException when the date is not part of the month
   */
  DayEntry day(LocalDate date) {
    return days.get(requireInMonth(date));
  }

  /**
   * @return these days, with the entry of its date replaced
   * @throws IllegalArgumentException when the entry is not for a day of the month
   */
  MonthOfDays with(DayEntry entry) {
    var replaced = new TreeMap<>(days);
    replaced.put(requireInMonth(entry.date()), entry);
    return new MonthOfDays(month, replaced);
  }

  Hours totalHours(HourCategory category) {
    return days.values().stream().map(day -> day.hours(category)).reduce(Hours.ZERO, Hours::plus);
  }

  Kilometres totalKilometres() {
    return days.values().stream().map(DayEntry::kilometres).reduce(Kilometres.ZERO, Kilometres::plus);
  }

  /**
   * The days with hours in a category that asks for an explanation, but without one.
   */
  List<LocalDate> daysLackingRequiredExplanation() {
    return days.values().stream().filter(DayEntry::lacksRequiredExplanation).map(DayEntry::date).toList();
  }

  private LocalDate requireInMonth(LocalDate date) {
    Objects.requireNonNull(date, "date");
    if (!YearMonth.from(date).equals(month)) {
      throw new IllegalArgumentException("%s is not part of %s".formatted(date, month));
    }
    return date;
  }
}
