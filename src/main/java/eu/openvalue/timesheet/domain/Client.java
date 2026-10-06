package eu.openvalue.timesheet.domain;

import java.util.Objects;

public record Client(String name) {

  public Client {
    Objects.requireNonNull(name, "name");
    name = name.strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Client name cannot be blank");
    }
  }
}
