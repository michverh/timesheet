package eu.openvalue.timesheet.domain;

/**
 * The kinds of hours a professional can book, matching the columns of the timesheet.
 */
public enum HourCategory {
  WORKED(true, false),
  OVERTIME(false, false),
  SICK(true, false),
  LEAVE(true, false),
  PUBLIC_HOLIDAY(true, false),
  AVAILABLE(true, false),
  TRAINING(true, true),
  OTHER(true, true),
  STAND_BY(false, false);

  private final boolean fillsWorkday;
  private final boolean requiresExplanation;

  HourCategory(boolean fillsWorkday, boolean requiresExplanation) {
    this.fillsWorkday = fillsWorkday;
    this.requiresExplanation = requiresExplanation;
  }

  /**
   * Whether hours in this category fill the standard workday, unlike overtime and stand-by, which come on top of it.
   */
  public boolean fillsWorkday() {
    return fillsWorkday;
  }

  /**
   * Whether hours in this category should be accompanied by an explanation of what they were spent on.
   */
  public boolean requiresExplanation() {
    return requiresExplanation;
  }
}
