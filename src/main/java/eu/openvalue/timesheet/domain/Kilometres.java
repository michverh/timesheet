package eu.openvalue.timesheet.domain;

/**
 * A whole number of kilometres driven, used for mileage reimbursement.
 */
public record Kilometres(int value) {

  public static final Kilometres ZERO = new Kilometres(0);

  public Kilometres {
    if (value < 0) {
      throw new IllegalArgumentException("Kilometres cannot be negative: " + value);
    }
  }

  public static Kilometres of(int value) {
    return new Kilometres(value);
  }

  public boolean isZero() {
    return value == 0;
  }
}
