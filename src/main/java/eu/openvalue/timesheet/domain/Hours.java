package eu.openvalue.timesheet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A non-negative amount of time booked on a timesheet, precise to the minute.
 */
public final class Hours implements Comparable<Hours> {

  private static final int MINUTES_PER_HOUR = 60;
  private static final BigDecimal MINUTES_PER_HOUR_DECIMAL = BigDecimal.valueOf(MINUTES_PER_HOUR);
  private static final int DECIMAL_HOURS_SCALE = 10;

  public static final Hours ZERO = new Hours(0);

  private final int minutes;

  private Hours(int minutes) {
    if (minutes < 0) {
      throw new IllegalArgumentException("Hours cannot be negative");
    }
    this.minutes = minutes;
  }

  public static Hours of(int wholeHours) {
    return new Hours(Math.multiplyExact(wholeHours, MINUTES_PER_HOUR));
  }

  /**
   * @throws IllegalArgumentException when the minutes are not between 0 and 59, or the result is negative
   */
  public static Hours of(int hours, int minutes) {
    if (minutes < 0 || minutes >= MINUTES_PER_HOUR) {
      throw new IllegalArgumentException("Minutes must be between 0 and 59: " + minutes);
    }
    return new Hours(Math.addExact(Math.multiplyExact(hours, MINUTES_PER_HOUR), minutes));
  }

  public static Hours ofMinutes(int minutes) {
    return new Hours(minutes);
  }

  /**
   * Hours given as a decimal number, rounded to the nearest minute: 7.5 is 7:30, and 7.3333 is 7:20.
   *
   * @throws IllegalArgumentException when the hours are negative or too large
   */
  public static Hours ofDecimalHours(BigDecimal hours) {
    Objects.requireNonNull(hours, "hours");
    try {
      return new Hours(hours.multiply(MINUTES_PER_HOUR_DECIMAL).setScale(0, RoundingMode.HALF_UP).intValueExact());
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Too many hours: " + hours, e);
    }
  }

  /**
   * These hours as a decimal number: 7.5 for 7:30, and 7.3333333333 for 7:20.
   */
  public BigDecimal toDecimalHours() {
    return BigDecimal.valueOf(minutes)
        .divide(MINUTES_PER_HOUR_DECIMAL, DECIMAL_HOURS_SCALE, RoundingMode.HALF_UP)
        .stripTrailingZeros();
  }

  /**
   * The whole hours, without the remaining minutes: 7 for 7:30.
   */
  public int wholeHours() {
    return minutes / MINUTES_PER_HOUR;
  }

  /**
   * The minutes beyond the whole hours: 30 for 7:30.
   */
  public int remainingMinutes() {
    return minutes % MINUTES_PER_HOUR;
  }

  public int inMinutes() {
    return minutes;
  }

  public Hours plus(Hours other) {
    return new Hours(Math.addExact(minutes, other.minutes));
  }

  /**
   * @throws IllegalArgumentException when the other hours exceed these
   */
  public Hours minus(Hours other) {
    return new Hours(minutes - other.minutes);
  }

  public boolean isZero() {
    return minutes == 0;
  }

  public boolean isGreaterThan(Hours other) {
    return minutes > other.minutes;
  }

  @Override
  public int compareTo(Hours other) {
    return Integer.compare(minutes, other.minutes);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Hours hours && minutes == hours.minutes;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(minutes);
  }

  /**
   * Formats as hours and minutes, like {@code 7:05}.
   */
  @Override
  public String toString() {
    return "%d:%02d".formatted(wholeHours(), remainingMinutes());
  }
}
