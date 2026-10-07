package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Hours on a single day during which the professional was absent, as registered by HR.
 */
public record Absence(LocalDate date, AbsenceType type, Hours hours, Optional<Explanation> description) {

  public Absence {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(hours, "hours");
    Objects.requireNonNull(description, "description");
    if (hours.isZero()) {
      throw new IllegalArgumentException("An absence must last longer than zero hours");
    }
  }

  public static Absence of(LocalDate date, AbsenceType type, Hours hours) {
    return new Absence(date, type, hours, Optional.empty());
  }

  public static Absence of(LocalDate date, AbsenceType type, Hours hours, Explanation description) {
    return new Absence(date, type, hours, Optional.of(description));
  }

  /**
   * What the absence was for: its own description, or else the default explanation of its type.
   */
  public Optional<Explanation> explanation() {
    return description.or(type::defaultExplanation);
  }
}
