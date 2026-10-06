package eu.openvalue.timesheet.domain;

/**
 * A whole number of hours, as booked on a timesheet.
 */
public record Hours(int value) implements Comparable<Hours> {

  public static final Hours ZERO = new Hours(0);

  public Hours {
    if (value < 0) {
      throw new IllegalArgumentException("Hours cannot be negative: " + value);
    }
  }

  public static Hours of(int value) {
    return new Hours(value);
  }

  public Hours plus(Hours other) {
    return new Hours(value + other.value);
  }

  public boolean isZero() {
    return value == 0;
  }

  public boolean isGreaterThan(Hours other) {
    return value > other.value;
  }

  @Override
  public int compareTo(Hours other) {
    return Integer.compare(value, other.value);
  }
}
