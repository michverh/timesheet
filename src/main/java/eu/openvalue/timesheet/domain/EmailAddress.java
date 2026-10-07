package eu.openvalue.timesheet.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

  private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

  public EmailAddress {
    Objects.requireNonNull(value, "value");
    value = value.strip().toLowerCase(Locale.ROOT);
    if (!FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("Not a valid email address: " + value);
    }
  }

  public String localPart() {
    return value.substring(0, value.indexOf('@'));
  }

  @Override
  public String toString() {
    return value;
  }
}
