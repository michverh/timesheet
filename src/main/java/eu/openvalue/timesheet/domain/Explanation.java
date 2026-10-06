package eu.openvalue.timesheet.domain;

import java.util.Objects;

/**
 * Free-text explanation of the hours booked on a day ("Toelichting").
 */
public record Explanation(String text) {

  public Explanation {
    Objects.requireNonNull(text, "text");
    text = text.strip();
    if (text.isEmpty()) {
      throw new IllegalArgumentException("Explanation cannot be blank");
    }
  }
}
