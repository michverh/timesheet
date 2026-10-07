package eu.openvalue.timesheet.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What a professional booked on a single day: hours per category, kilometres driven and an optional explanation.
 */
public final class DayEntry {

  static final Hours MAXIMUM_DAILY_HOURS = Hours.of(24);

  private final LocalDate date;
  private final Map<HourCategory, Hours> hours;
  private final Kilometres kilometres;
  private final Explanation explanation;

  private DayEntry(LocalDate date, Map<HourCategory, Hours> hours, Kilometres kilometres, Explanation explanation) {
    this.date = date;
    this.hours = hours;
    this.kilometres = kilometres;
    this.explanation = explanation;
  }

  static DayEntry empty(LocalDate date) {
    return new DayEntry(Objects.requireNonNull(date, "date"), new EnumMap<>(HourCategory.class), Kilometres.ZERO, null);
  }

  public LocalDate date() {
    return date;
  }

  public boolean isWeekend() {
    var dayOfWeek = date.getDayOfWeek();
    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
  }

  public Hours hours(HourCategory category) {
    return hours.getOrDefault(category, Hours.ZERO);
  }

  /**
   * The non-zero hours booked on this day, per category.
   */
  public Map<HourCategory, Hours> bookedHours() {
    return Collections.unmodifiableMap(hours);
  }

  public Hours totalHours() {
    return total(hours);
  }

  public Kilometres kilometres() {
    return kilometres;
  }

  public Optional<Explanation> explanation() {
    return Optional.ofNullable(explanation);
  }

  public boolean lacksRequiredExplanation() {
    return explanation == null && hours.keySet().stream().anyMatch(HourCategory::requiresExplanation);
  }

  DayEntry withHours(HourCategory category, Hours newHours) {
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(newHours, "newHours");
    var updated = new EnumMap<HourCategory, Hours>(HourCategory.class);
    updated.putAll(hours);
    updated.put(category, newHours);
    return withBookedHours(updated);
  }

  DayEntry withBookedHours(Map<HourCategory, Hours> newHours) {
    var booked = new EnumMap<HourCategory, Hours>(HourCategory.class);
    newHours.forEach((category, hours) -> {
      if (!Objects.requireNonNull(hours, "hours").isZero()) {
        booked.put(Objects.requireNonNull(category, "category"), hours);
      }
    });
    if (total(booked).isGreaterThan(MAXIMUM_DAILY_HOURS)) {
      throw new DailyHoursLimitExceededException(date, MAXIMUM_DAILY_HOURS);
    }
    return new DayEntry(date, booked, kilometres, explanation);
  }

  DayEntry withKilometres(Kilometres newKilometres) {
    return new DayEntry(date, hours, Objects.requireNonNull(newKilometres, "newKilometres"), explanation);
  }

  DayEntry withExplanation(Explanation newExplanation) {
    return new DayEntry(date, hours, kilometres, newExplanation);
  }

  private static Hours total(Map<HourCategory, Hours> hours) {
    return hours.values().stream().reduce(Hours.ZERO, Hours::plus);
  }
}
