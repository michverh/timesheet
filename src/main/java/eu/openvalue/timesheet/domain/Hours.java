package eu.openvalue.timesheet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A non-negative amount of time booked on a timesheet, precise to the minute.
 */
public final class Hours implements Comparable<Hours> {

  private static final int MINUTES_PER_HOUR = 60;
  private static final BigDecimal MINUTES_PER_HOUR_DECIMAL = BigDecimal.valueOf(MINUTES_PER_HOUR);
  private static final int DECIMAL_HOURS_SCALE = 10;
  private static final Pattern HOURS_AND_MINUTES = Pattern.compile("(\\d{1,3}):(\\d{1,2})");
  private static final Pattern DECIMAL_HOURS = Pattern.compile("\\d{1,3}([.,]\\d+)?");

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
   * Reads hours as people write them: as hours and minutes ({@code 7:30}), as whole hours ({@code 8}) or as decimal
   * hours ({@code 7.5} or {@code 7,5}, rounded to the nearest minute).
   *
   * @throws IllegalArgumentException when the text is not a non-negative amount of hours written in one of these ways
   */
  public static Hours parse(String text) {
    var input = Objects.requireNonNull(text, "text").strip();
    var hoursAndMinutes = HOURS_AND_MINUTES.matcher(input);
    if (hoursAndMinutes.matches()) {
      return of(Integer.parseInt(hoursAndMinutes.group(1)), Integer.parseInt(hoursAndMinutes.group(2)));
    }
    if (DECIMAL_HOURS.matcher(input).matches()) {
      return ofDecimalHours(new BigDecimal(input.replace(',', '.')));
    }
    throw new IllegalArgumentException("Not an amount of hours: " + text);
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
  public static Hours min(Hours some, Hours other) {
    return some.compareTo(other) <= 0 ? some : other;
  }

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
