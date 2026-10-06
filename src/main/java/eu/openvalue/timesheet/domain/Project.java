package eu.openvalue.timesheet.domain;

import java.util.Objects;

public record Project(String name) {

  public Project {
    Objects.requireNonNull(name, "name");
    name = name.strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Project name cannot be blank");
    }
  }
}
