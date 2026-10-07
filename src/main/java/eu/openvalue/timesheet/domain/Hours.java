package eu.openvalue.timesheet.domain;

/**
 * A non-negative amount of time booked on a timesheet, precise to the minute.
 */
public final class Hours implements Comparable<Hours> {

  private static final int MINUTES_PER_HOUR = 60;

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
