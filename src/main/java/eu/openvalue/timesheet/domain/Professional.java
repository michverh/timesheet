package eu.openvalue.timesheet.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The person whose hours a timesheet accounts for, identified by a company email address of the form
 * {@code firstname.lastname@...}.
 */
public record Professional(EmailAddress email) {

  private static final Set<String> SURNAME_PARTICLES =
      Set.of("van", "de", "der", "den", "het", "ten", "ter", "te", "in", "op", "aan", "'t");

  public Professional {
    Objects.requireNonNull(email, "email");
    var nameParts = email.localPart().split("\\.", -1);
    if (nameParts.length < 2 || Arrays.stream(nameParts).anyMatch(String::isEmpty)) {
      throw new IllegalArgumentException("Expected an email address of the form firstname.lastname: " + email);
    }
  }

  public static Professional withEmail(String email) {
    return new Professional(new EmailAddress(email));
  }

  /**
   * The display name derived from the email address, e.g. {@code jan.van.der.berg@...} becomes "Jan van der Berg".
   */
  public String name() {
    var nameParts = email.localPart().split("\\.");
    var name = new StringBuilder(capitalise(nameParts[0]));
    for (int i = 1; i < nameParts.length; i++) {
      var part = nameParts[i];
      var isParticle = i < nameParts.length - 1 && SURNAME_PARTICLES.contains(part);
      name.append(' ').append(isParticle ? part : capitalise(part));
    }
    return name.toString();
  }

  private static String capitalise(String namePart) {
    return Arrays.stream(namePart.split("-", -1))
        .map(segment -> segment.isEmpty() ? segment
            : segment.substring(0, 1).toUpperCase(Locale.ROOT) + segment.substring(1))
        .collect(Collectors.joining("-"));
  }
}
