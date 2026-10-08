package eu.openvalue.timesheet.adapter.out.absence;

import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistryUnavailableException;
import eu.openvalue.timesheet.domain.Absence;
import eu.openvalue.timesheet.domain.AbsenceType;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Retrieves registered absences from HR's absence registry:
 * {@code GET /professionals/{email}/absences?month=YYYY-MM}, answered with one entry per day of absence.
 */
public final class HttpAbsenceRegistry implements AbsenceRegistry {

  /**
   * The registry's absence types and how this application books them. Kept apart from the names of
   * {@link AbsenceType}, so either side can rename without silently breaking the other.
   */
  private static final Map<String, AbsenceType> REGISTRY_TYPES = Map.of(
      "LEAVE", AbsenceType.LEAVE,
      "SICK", AbsenceType.SICK,
      "TRAINING", AbsenceType.TRAINING,
      "SPECIAL_LEAVE", AbsenceType.SPECIAL_LEAVE,
      "OTHER", AbsenceType.OTHER);

  private final RestClient restClient;

  private HttpAbsenceRegistry(RestClient restClient) {
    this.restClient = restClient;
  }

  /**
   * @param timeout how long to wait for the registry to connect, and to answer
   */
  public static HttpAbsenceRegistry connectingTo(URI baseUrl, Duration timeout) {
    Objects.requireNonNull(baseUrl, "baseUrl");
    Objects.requireNonNull(timeout, "timeout");
    var requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
    requestFactory.setReadTimeout(timeout);
    return new HttpAbsenceRegistry(RestClient.builder()
        .baseUrl(baseUrl.toString())
        .requestFactory(requestFactory)
        .build());
  }

  @Override
  public List<Absence> absencesOf(Professional professional, YearMonth month) {
    var registered = fetch(professional, month);
    try {
      return registered.absences().stream().map(HttpAbsenceRegistry::toAbsence).toList();
    } catch (IllegalArgumentException e) {
      throw new AbsenceRegistryUnavailableException(
          "The absence registry answered with invalid absences of %s for %s".formatted(professional.email(), month),
          e);
    }
  }

  /**
   * @return the registered absences; none when the registry does not know the professional
   */
  private RegisteredAbsences fetch(Professional professional, YearMonth month) {
    try {
      return request(professional, month);
    } catch (RestClientException e) {
      throw new AbsenceRegistryUnavailableException(
          "Could not retrieve the absences of %s for %s".formatted(professional.email(), month), e);
    }
  }

  private RegisteredAbsences request(Professional professional, YearMonth month) {
    return restClient.get()
        .uri("/professionals/{email}/absences?month={month}", professional.email().value(), month)
        .exchange((request, response) -> {
          if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return RegisteredAbsences.NONE;
          }
          if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("The absence registry answered " + response.getStatusCode());
          }
          var body = response.bodyTo(RegisteredAbsences.class);
          if (body == null) {
            throw new RestClientException("The absence registry answered without absences");
          }
          return body;
        });
  }

  /**
   * @throws IllegalArgumentException when the registered absence is incomplete or invalid
   */
  private static Absence toAbsence(RegisteredAbsence registered) {
    if (registered.date() == null || registered.type() == null || registered.hours() == null) {
      throw new IllegalArgumentException("Incomplete absence: " + registered);
    }
    var hours = Hours.ofDecimalHours(registered.hours());
    var type = REGISTRY_TYPES.get(registered.type());
    if (type == null) {
      return ofUnknownType(registered, hours);
    }
    return new Absence(registered.date(), type, hours, descriptionOf(registered));
  }

  /**
   * Books an absence of a type the registry has introduced since, as other hours explained by that type unless the
   * absence itself is described.
   */
  private static Absence ofUnknownType(RegisteredAbsence registered, Hours hours) {
    var explanation = descriptionOf(registered).orElseGet(() -> new Explanation(registered.type()));
    return Absence.of(registered.date(), AbsenceType.OTHER, hours, explanation);
  }

  private static Optional<Explanation> descriptionOf(RegisteredAbsence registered) {
    return Optional.ofNullable(registered.description()).filter(text -> !text.isBlank()).map(Explanation::new);
  }

  record RegisteredAbsences(List<RegisteredAbsence> absences) {

    static final RegisteredAbsences NONE = new RegisteredAbsences(List.of());

    RegisteredAbsences {
      absences = absences == null ? List.of() : List.copyOf(absences);
    }
  }

  record RegisteredAbsence(LocalDate date, String type, BigDecimal hours, String description) {
  }
}
