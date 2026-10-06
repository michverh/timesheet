package eu.openvalue.timesheet.domain;

/**
 * The kinds of hours a professional can book, matching the columns of the timesheet.
 */
public enum HourCategory {
  WORKED,
  OVERTIME,
  SICK,
  LEAVE,
  PUBLIC_HOLIDAY,
  AVAILABLE,
  TRAINING(true),
  OTHER(true),
  STAND_BY;

  private final boolean requiresExplanation;

  HourCategory() {
    this(false);
  }

  HourCategory(boolean requiresExplanation) {
    this.requiresExplanation = requiresExplanation;
  }

  /**
   * Whether hours in this category should be accompanied by an explanation of what they were spent on.
   */
  public boolean requiresExplanation() {
    return requiresExplanation;
  }
}
