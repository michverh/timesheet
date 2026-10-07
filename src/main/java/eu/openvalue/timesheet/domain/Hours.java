package eu.openvalue.timesheet.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A non-negative amount of time booked on a timesheet, in multiples of a quarter of an hour.
 */
public final class Hours implements Comparable<Hours> {

  private static final int QUARTERS_PER_HOUR = 4;
  private static final BigDecimal QUARTERS_PER_HOUR_DECIMAL = BigDecimal.valueOf(QUARTERS_PER_HOUR);

  public static final Hours ZERO = new Hours(0);

  private final int quarters;

  private Hours(int quarters) {
    if (quarters < 0) {
      throw new IllegalArgumentException("Hours cannot be negative");
    }
    this.quarters = quarters;
  }

  public static Hours of(int wholeHours) {
    return new Hours(Math.multiplyExact(wholeHours, QUARTERS_PER_HOUR));
  }

  /**
   * @throws IllegalArgumentException when the hours are negative or not a multiple of a quarter of an hour
   */
  public static Hours of(BigDecimal hours) {
    Objects.requireNonNull(hours, "hours");
    var quarters = hours.multiply(QUARTERS_PER_HOUR_DECIMAL);
    if (quarters.stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException("Hours must be a multiple of a quarter of an hour: " + hours);
    }
    return new Hours(quarters.intValueExact());
  }

  public BigDecimal toDecimal() {
    return BigDecimal.valueOf(quarters).divide(QUARTERS_PER_HOUR_DECIMAL).stripTrailingZeros();
  }

  public Hours plus(Hours other) {
    return new Hours(Math.addExact(quarters, other.quarters));
  }

  /**
   * @throws IllegalArgumentException when the other hours exceed these
   */
  public Hours minus(Hours other) {
    return new Hours(quarters - other.quarters);
  }

  public boolean isZero() {
    return quarters == 0;
  }

  public boolean isGreaterThan(Hours other) {
    return quarters > other.quarters;
  }

  @Override
  public int compareTo(Hours other) {
    return Integer.compare(quarters, other.quarters);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Hours hours && quarters == hours.quarters;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(quarters);
  }

  @Override
  public String toString() {
    return toDecimal().toPlainString() + "h";
  }
}
