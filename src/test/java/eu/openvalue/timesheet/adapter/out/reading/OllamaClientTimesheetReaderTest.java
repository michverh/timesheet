package eu.openvalue.timesheet.adapter.out.reading;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.json.JsonMapper;

class OllamaClientTimesheetReaderTest {

  private static final String MODEL = "qwen3-vl:8b-instruct";
  private static final byte[] IMAGE = {1, 2, 3, 4};
  private static final ClientTimesheetDocument SCREENSHOT =
      new ClientTimesheetDocument("week-27.png", DocumentFormat.PNG, IMAGE);

  @RegisterExtension
  static final WireMockExtension OLLAMA = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  private OllamaClientTimesheetReader reader;

  @BeforeEach
  void connect() {
    reader = OllamaClientTimesheetReader.connectingTo(URI.create(OLLAMA.baseUrl()), MODEL, Duration.ofSeconds(5));
  }

  @Test
  void asksTheModelToTranscribeTheImageInAFixedStructure() {
    // Given
    answer("""
        {"year": 2026, "days": []}""");

    // When
    reader.read(SCREENSHOT);

    // Then
    OLLAMA.verify(postRequestedFor(urlPathEqualTo("/api/generate"))
        .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL)))
        .withRequestBody(matchingJsonPath("$.images[0]", equalTo(Base64.getEncoder().encodeToString(IMAGE))))
        .withRequestBody(matchingJsonPath("$.stream", equalTo("false")))
        .withRequestBody(matchingJsonPath("$.options.temperature", equalTo("0")))
        .withRequestBody(matchingJsonPath("$.format.required[1]", equalTo("days"))));
  }

  @Test
  void givesTheHoursWorkedPerDayAsTranscribed() {
    // Given: week 27 of 2026, as the model transcribed it
    answer("""
        {"year": 2026, "days": [
          {"dayOfMonth": 29, "month": 6, "hoursWorked": "08:00"},
          {"dayOfMonth": 30, "month": 6, "hoursWorked": "7:30"},
          {"dayOfMonth": 1, "month": 7, "hoursWorked": "-"},
          {"dayOfMonth": 2, "month": 7, "hoursWorked": "7,5"},
          {"dayOfMonth": 3, "month": 7, "hoursWorked": ""}
        ]}""");

    // When
    var reported = reader.read(SCREENSHOT);

    // Then
    assertThat(reported).isEqualTo(new ClientReportedHours(Map.of(
        LocalDate.of(2026, 6, 29), Hours.of(8),
        LocalDate.of(2026, 6, 30), Hours.of(7, 30),
        LocalDate.of(2026, 7, 1), Hours.ZERO,
        LocalDate.of(2026, 7, 2), Hours.of(7, 30),
        LocalDate.of(2026, 7, 3), Hours.ZERO)));
  }

  @Test
  void movesOnToTheNextYearWhenAWeekPassesNewYear() {
    // Given
    answer("""
        {"year": 2026, "days": [
          {"dayOfMonth": 31, "month": 12, "hoursWorked": "8"},
          {"dayOfMonth": 1, "month": 1, "hoursWorked": ""}
        ]}""");

    // When
    var reported = reader.read(SCREENSHOT);

    // Then
    assertThat(reported.on(LocalDate.of(2027, 1, 1))).contains(Hours.ZERO);
  }

  @Test
  void readsJpegImagesToo() {
    // Given
    answer("""
        {"year": 2026, "days": [{"dayOfMonth": 1, "month": 7, "hoursWorked": "8"}]}""");

    // When
    var reported = reader.read(new ClientTimesheetDocument("week.jpg", DocumentFormat.JPEG, IMAGE));

    // Then
    assertThat(reported.on(LocalDate.of(2026, 7, 1))).contains(Hours.of(8));
  }

  @Test
  void doesNotReadPdfDocumentsYet() {
    assertThatThrownBy(() -> reader.read(new ClientTimesheetDocument("acme.pdf", DocumentFormat.PDF, IMAGE)))
        .isInstanceOf(ClientTimesheetUnreadableException.class)
        .hasMessageContaining("not supported yet");
  }

  @Test
  void reportsAModelThatCannotBeReached() {
    // Given
    OLLAMA.stubFor(post(urlPathEqualTo("/api/generate")).willReturn(serverError()));

    // When / Then
    assertThatThrownBy(() -> reader.read(SCREENSHOT)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsATranscriptionThatIsNotInTheFixedStructure() {
    // Given
    answer("The timesheet shows 32 hours.");

    // When / Then
    assertThatThrownBy(() -> reader.read(SCREENSHOT)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsATranscriptionWithSomethingOtherThanHours() {
    // Given
    answer("""
        {"year": 2026, "days": [{"dayOfMonth": 1, "month": 7, "hoursWorked": "sick"}]}""");

    // When / Then
    assertThatThrownBy(() -> reader.read(SCREENSHOT)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsATranscriptionWithADayThatDoesNotExist() {
    // Given
    answer("""
        {"year": 2026, "days": [{"dayOfMonth": 30, "month": 2, "hoursWorked": "8"}]}""");

    // When / Then
    assertThatThrownBy(() -> reader.read(SCREENSHOT)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  /**
   * Lets Ollama answer as it does without streaming: the transcription is a string inside its response.
   */
  private static void answer(String transcription) {
    var answer = JsonMapper.builder().build()
        .writeValueAsString(Map.of("model", MODEL, "response", transcription, "done", true));
    OLLAMA.stubFor(post(urlPathEqualTo("/api/generate")).willReturn(okJson(answer)));
  }
}
