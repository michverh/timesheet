package eu.openvalue.timesheet.adapter.out.reading;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reads the examples with the Ollama stub from compose.yaml, which answers with what the model actually read from them.
 * Fails when the requests for the examples no longer match the recordings, for instance after changing how PDFs are
 * rendered or spreadsheets transcribed; wiremock/ollama/README.md explains how to record them again.
 */
class RecordedOllamaReadingsTest {

  private static final Path STUB = Path.of("wiremock", "ollama");
  private static final int[] DAYS_WORKED_IN_AUGUST = {3, 4, 6, 7, 10, 11, 13, 14, 17, 18, 20, 21, 24, 25, 27, 31};

  @RegisterExtension
  static final WireMockExtension OLLAMA = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort().usingFilesUnderDirectory(STUB.toString()))
      .build();

  @Test
  void readsTheWeekFromTheScreenshot() throws IOException {
    // When
    var reported = reader().read(example("week-27-2026.png", DocumentFormat.PNG));

    // Then
    assertThat(reported).isEqualTo(new ClientReportedHours(Map.of(
        LocalDate.of(2026, 6, 29), Hours.of(8),
        LocalDate.of(2026, 6, 30), Hours.of(8),
        LocalDate.of(2026, 7, 1), Hours.ZERO,
        LocalDate.of(2026, 7, 2), Hours.of(8),
        LocalDate.of(2026, 7, 3), Hours.of(8),
        LocalDate.of(2026, 7, 4), Hours.ZERO,
        LocalDate.of(2026, 7, 5), Hours.ZERO)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pdf", "xlsx"})
  void readsTheMonthFromTheTimesheet(String extension) throws IOException {
    // Given
    var format = DocumentFormat.valueOf(extension.toUpperCase());

    // When
    var reported = reader().read(example("2026-08-jan-jansen." + extension, format));

    // Then
    assertThat(reported).isEqualTo(august());
  }

  @Test
  void cannotReadADocumentWithoutRecordedReading() {
    // Given
    var other = new ClientTimesheetDocument("other.png", DocumentFormat.PNG, new byte[] {1, 2, 3});

    // When / Then
    assertThatThrownBy(() -> reader().read(other)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  private static TranscribingClientTimesheetReader reader() {
    return new TranscribingClientTimesheetReader(OllamaTranscriber.connectingTo(URI.create(OLLAMA.baseUrl()),
        "qwen3-vl:8b-instruct", Duration.ofSeconds(10)));
  }

  private static ClientTimesheetDocument example(String fileName, DocumentFormat format) throws IOException {
    return new ClientTimesheetDocument(fileName, format, Files.readAllBytes(STUB.resolve("examples").resolve(fileName)));
  }

  private static ClientReportedHours august() {
    var hours = new TreeMap<LocalDate, Hours>();
    LocalDate.of(2026, 8, 1).datesUntil(LocalDate.of(2026, 9, 1)).forEach(date -> hours.put(date, Hours.ZERO));
    for (var day : DAYS_WORKED_IN_AUGUST) {
      hours.put(LocalDate.of(2026, 8, day), Hours.of(8));
    }
    return new ClientReportedHours(hours);
  }
}
