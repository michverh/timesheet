package eu.openvalue.timesheet.adapter.out.absence;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistryUnavailableException;
import eu.openvalue.timesheet.domain.Absence;
import eu.openvalue.timesheet.domain.AbsenceType;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HttpAbsenceRegistryTest {

  private static final Professional JAN = Professional.withEmail("jan.van.der.berg@openvalue.nl");
  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final String ABSENCES_OF_JAN = "/professionals/jan.van.der.berg%40openvalue.nl/absences";
  private static final Duration TIMEOUT = Duration.ofMillis(500);

  @RegisterExtension
  static final WireMockExtension REGISTRY = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  private HttpAbsenceRegistry registry;

  @BeforeEach
  void connect() {
    registry = HttpAbsenceRegistry.connectingTo(URI.create(REGISTRY.baseUrl()), TIMEOUT);
  }

  @Test
  void asksForTheAbsencesOfTheProfessionalDuringTheMonth() {
    // Given
    REGISTRY.stubFor(get(urlPathMatching(".*")).willReturn(okJson("{\"absences\": []}")));

    // When
    registry.absencesOf(JAN, MAY_2026);

    // Then
    REGISTRY.verify(getRequestedFor(urlPathEqualTo(ABSENCES_OF_JAN)).withQueryParam("month", equalTo("2026-05")));
  }

  @Test
  void givesTheRegisteredAbsences() {
    // Given
    stubAbsences("""
        {
          "professionalId": "jan.van.der.berg@openvalue.nl",
          "month": "2026-05",
          "absences": [
            { "date": "2026-05-04", "type": "LEAVE", "hours": 8 },
            { "date": "2026-05-05", "type": "OTHER", "hours": 2.5, "description": "Doctor appointment" },
            { "date": "2026-05-06", "type": "SPECIAL_LEAVE", "hours": 8 }
          ]
        }""");

    // When
    var absences = registry.absencesOf(JAN, MAY_2026);

    // Then
    assertThat(absences).containsExactly(
        Absence.of(LocalDate.of(2026, 5, 4), AbsenceType.LEAVE, Hours.of(8)),
        Absence.of(LocalDate.of(2026, 5, 5), AbsenceType.OTHER, Hours.of(2, 30), new Explanation("Doctor appointment")),
        Absence.of(LocalDate.of(2026, 5, 6), AbsenceType.SPECIAL_LEAVE, Hours.of(8)));
  }

  @Test
  void roundsDecimalHoursToTheNearestMinute() {
    // Given
    stubAbsences("{\"absences\": [{ \"date\": \"2026-05-04\", \"type\": \"SICK\", \"hours\": 7.3333 }]}");

    // When / Then
    assertThat(registry.absencesOf(JAN, MAY_2026)).singleElement()
        .extracting(Absence::hours).isEqualTo(Hours.of(7, 20));
  }

  @Test
  void booksAbsencesOfAnUnknownTypeAsOtherExplainedByTheirType() {
    // Given
    stubAbsences("{\"absences\": [{ \"date\": \"2026-05-04\", \"type\": \"PARENTAL_LEAVE\", \"hours\": 8 }]}");

    // When / Then
    assertThat(registry.absencesOf(JAN, MAY_2026)).containsExactly(
        Absence.of(LocalDate.of(2026, 5, 4), AbsenceType.OTHER, Hours.of(8), new Explanation("PARENTAL_LEAVE")));
  }

  @Test
  void givesNoAbsencesForAProfessionalUnknownToTheRegistry() {
    // Given
    REGISTRY.stubFor(get(urlPathEqualTo(ABSENCES_OF_JAN)).willReturn(notFound()));

    // When / Then
    assertThat(registry.absencesOf(JAN, MAY_2026)).isEmpty();
  }

  @Test
  void reportsAFailingRegistry() {
    // Given
    REGISTRY.stubFor(get(urlPathEqualTo(ABSENCES_OF_JAN)).willReturn(serverError()));

    // When / Then
    assertThatThrownBy(() -> registry.absencesOf(JAN, MAY_2026))
        .isInstanceOf(AbsenceRegistryUnavailableException.class);
  }

  @Test
  void reportsARegistryThatAnswersTooLate() {
    // Given
    REGISTRY.stubFor(get(urlPathEqualTo(ABSENCES_OF_JAN))
        .willReturn(okJson("{\"absences\": []}").withFixedDelay((int) TIMEOUT.toMillis() * 4)));

    // When / Then
    assertThatThrownBy(() -> registry.absencesOf(JAN, MAY_2026))
        .isInstanceOf(AbsenceRegistryUnavailableException.class);
  }

  @Test
  void reportsAnUnreadableAnswer() {
    // Given
    REGISTRY.stubFor(get(urlPathEqualTo(ABSENCES_OF_JAN)).willReturn(aResponse()
        .withHeader("Content-Type", "application/json").withBody("{ not json")));

    // When / Then
    assertThatThrownBy(() -> registry.absencesOf(JAN, MAY_2026))
        .isInstanceOf(AbsenceRegistryUnavailableException.class);
  }

  @Test
  void reportsAnIncompleteAbsence() {
    // Given
    stubAbsences("{\"absences\": [{ \"date\": \"2026-05-04\", \"type\": \"LEAVE\" }]}");

    // When / Then
    assertThatThrownBy(() -> registry.absencesOf(JAN, MAY_2026))
        .isInstanceOf(AbsenceRegistryUnavailableException.class);
  }

  @Test
  void reportsAnAbsenceWithoutHours() {
    // Given
    stubAbsences("{\"absences\": [{ \"date\": \"2026-05-04\", \"type\": \"LEAVE\", \"hours\": 0 }]}");

    // When / Then
    assertThatThrownBy(() -> registry.absencesOf(JAN, MAY_2026))
        .isInstanceOf(AbsenceRegistryUnavailableException.class);
  }

  private static void stubAbsences(String json) {
    REGISTRY.stubFor(get(urlPathEqualTo(ABSENCES_OF_JAN)).withQueryParam("month", equalTo("2026-05"))
        .willReturn(okJson(json)));
  }
}
