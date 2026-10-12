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

  /**
   * The hours that fill a weekday.
   */
  public static final Hours STANDARD_WORKDAY = Hours.of(8);

  private final LocalDate date;
  private final Map<HourCategory, Hours> hours;
  private final Kilometres kilometres;
  private final Explanation explanation;
  private final boolean leaveSuggested;

  private DayEntry(LocalDate date, Map<HourCategory, Hours> hours, Kilometres kilometres, Explanation explanation,
      boolean leaveSuggested) {
    this.date = date;
    this.hours = hours;
    this.kilometres = kilometres;
    this.explanation = explanation;
    this.leaveSuggested = leaveSuggested;
  }

  /**
   * A day as it was booked, for instance as kept by a repository.
   *
   * @throws DailyHoursLimitExceededException when the day's total exceeds 24 hours
   */
  public static DayEntry of(LocalDate date, Map<HourCategory, Hours> hours, Kilometres kilometres,
      Optional<Explanation> explanation) {
    return of(date, hours, kilometres, explanation, false);
  }

  /**
   * A day as it was booked, for instance as kept by a repository.
   *
   * @param leaveSuggested whether its leave was filled in for hours not worked, and is yet to be confirmed
   * @throws DailyHoursLimitExceededException when the day's total exceeds 24 hours
   */
  public static DayEntry of(LocalDate date, Map<HourCategory, Hours> hours, Kilometres kilometres,
      Optional<Explanation> explanation, boolean leaveSuggested) {
    Objects.requireNonNull(explanation, "explanation");
    var day = empty(date).withBookedHours(Objects.requireNonNull(hours, "hours"))
        .withKilometres(kilometres)
        .withExplanation(explanation.orElse(null));
    return new DayEntry(day.date, day.hours, day.kilometres, day.explanation,
        leaveSuggested && !day.hours(HourCategory.LEAVE).isZero());
  }

  static DayEntry empty(LocalDate date) {
    return new DayEntry(Objects.requireNonNull(date, "date"), new EnumMap<>(HourCategory.class), Kilometres.ZERO, null,
        false);
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

  /**
   * The hours in the categories that fill the workday: all but overtime and stand-by.
   */
  public Hours workdayHours() {
    return hours.entrySet().stream()
        .filter(booked -> booked.getKey().fillsWorkday())
        .map(Map.Entry::getValue)
        .reduce(Hours.ZERO, Hours::plus);
  }

  /**
   * Whether this is a weekday on which the hours filling the workday add up to more or less than the standard
   * workday.
   */
  public boolean deviatesFromStandardWorkday() {
    return !isWeekend() && !workdayHours().equals(STANDARD_WORKDAY);
  }

  /**
   * Whether the hours can be booked in the category, instead of the hours booked there now, without exceeding 24
   * hours on the day.
   */
  boolean canBook(HourCategory category, Hours newHours) {
    return !totalHours().minus(hours(category)).plus(newHours).isGreaterThan(MAXIMUM_DAILY_HOURS);
  }

  /**
   * Whether the leave on this day was filled in for hours that were expected, but not worked, and the professional
   * has yet to confirm it by changing the day.
   */
  public boolean isLeaveSuggested() {
    return leaveSuggested;
  }

  /**
   * The hours missing to fill a standard workday on a weekday; none on a weekend.
   */
  Hours shortOfStandardWorkday() {
    var booked = workdayHours();
    return isWeekend() || !STANDARD_WORKDAY.isGreaterThan(booked) ? Hours.ZERO : STANDARD_WORKDAY.minus(booked);
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
    return new DayEntry(date, booked, kilometres, explanation, leaveSuggested);
  }

  DayEntry withKilometres(Kilometres newKilometres) {
    return new DayEntry(date, hours, Objects.requireNonNull(newKilometres, "newKilometres"), explanation,
        leaveSuggested);
  }

  DayEntry withExplanation(Explanation newExplanation) {
    return new DayEntry(date, hours, kilometres, newExplanation, leaveSuggested);
  }

  /**
   * @return this day with the leave booked, as filled in for hours not worked
   */
  DayEntry withSuggestedLeave(Hours leave) {
    var suggested = withHours(HourCategory.LEAVE, leave);
    return new DayEntry(date, suggested.hours, kilometres, explanation, true);
  }

  /**
   * @return this day without the leave filled in for hours not worked, when it has not been confirmed
   */
  DayEntry withoutSuggestedLeave() {
    if (!leaveSuggested) {
      return this;
    }
    var withdrawn = withHours(HourCategory.LEAVE, Hours.ZERO);
    return new DayEntry(date, withdrawn.hours, kilometres, explanation, false);
  }

  /**
   * @return this day as confirmed by the professional, including any leave filled in for them
   */
  DayEntry confirmed() {
    return new DayEntry(date, hours, kilometres, explanation, false);
  }

  private static Hours total(Map<HourCategory, Hours> hours) {
    return hours.values().stream().reduce(Hours.ZERO, Hours::plus);
  }
}
