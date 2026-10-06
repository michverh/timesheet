package eu.openvalue.timesheet.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * The client, and optionally the project, a professional works for during a month. A professional without an
 * assignment is on the bench.
 */
public record Assignment(Client client, Optional<Project> project) {

  public Assignment {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(project, "project");
  }

  public static Assignment at(Client client) {
    return new Assignment(client, Optional.empty());
  }

  public static Assignment at(Client client, Project project) {
    return new Assignment(client, Optional.of(project));
  }
}
