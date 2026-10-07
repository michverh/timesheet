package eu.openvalue.timesheet.domain;

import java.util.Optional;

/**
 * The kinds of absence registered for a professional, and the timesheet category each is booked in.
 */
public enum AbsenceType {
  LEAVE(HourCategory.LEAVE, null),
  SICK(HourCategory.SICK, null),
  TRAINING(HourCategory.TRAINING, null),
  SPECIAL_LEAVE(HourCategory.OTHER, "Bijzonder verlof"),
  OTHER(HourCategory.OTHER, null);

  private final HourCategory category;
  private final Explanation defaultExplanation;

  AbsenceType(HourCategory category, String defaultExplanation) {
    this.category = category;
    this.defaultExplanation = defaultExplanation == null ? null : new Explanation(defaultExplanation);
  }

  public HourCategory category() {
    return category;
  }

  /**
   * The explanation to put on the timesheet when the absence itself does not describe what it was for.
   */
  public Optional<Explanation> defaultExplanation() {
    return Optional.ofNullable(defaultExplanation);
  }
}
