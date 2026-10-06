package eu.openvalue.timesheet.domain;

import java.util.Objects;

/**
 * The person whose hours a timesheet accounts for.
 */
public record Professional(String name) {

  public Professional {
    Objects.requireNonNull(name, "name");
    name = name.strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Professional name cannot be blank");
    }
  }
}
