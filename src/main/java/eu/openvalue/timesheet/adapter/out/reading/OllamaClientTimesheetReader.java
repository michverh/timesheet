package eu.openvalue.timesheet.adapter.out.reading;

import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads client timesheets with a vision model served by Ollama. The model only transcribes what the timesheet says,
 * in a fixed structure; turning that into dates and hours is done here.
 */
public final class OllamaClientTimesheetReader implements ClientTimesheetReader {

  private static final Set<DocumentFormat> IMAGES = Set.of(DocumentFormat.PNG, DocumentFormat.JPEG);
  private static final Set<String> NO_HOURS = Set.of("", "-", "–");

  private static final String PROMPT = """
      This image is a timesheet in which a client registered the hours a consultant worked.
      List every day that has a column or row in the timesheet, in order, including days without hours.
      For each day give its day of the month, its month number and the hours worked exactly as written in the \
      timesheet (for example "08:00").
      Use an empty string when no hours are written for that day.""";

  private static final Map<String, Object> TRANSCRIPTION_SCHEMA = Map.of(
      "type", "object",
      "properties", Map.of(
          "year", Map.of("type", "integer", "description", "the year the timesheet concerns"),
          "days", Map.of("type", "array", "items", Map.of(
              "type", "object",
              "properties", Map.of(
                  "dayOfMonth", Map.of("type", "integer", "description", "day of the month, 1 to 31"),
                  "month", Map.of("type", "integer", "description", "month number, 1 to 12"),
                  "hoursWorked", Map.of("type", "string",
                      "description", "the hours worked on the day exactly as written, or empty")),
              "required", List.of("dayOfMonth", "month", "hoursWorked")))),
      "required", List.of("year", "days"));

  private final RestClient restClient;
  private final String model;
  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  private OllamaClientTimesheetReader(RestClient restClient, String model) {
    this.restClient = restClient;
    this.model = model;
  }

  /**
   * @param timeout how long to wait for Ollama to connect, and to answer; reading a timesheet can take a minute
   */
  public static OllamaClientTimesheetReader connectingTo(URI baseUrl, String model, Duration timeout) {
    Objects.requireNonNull(baseUrl, "baseUrl");
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(timeout, "timeout");
    var httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(timeout)
        .build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(timeout);
    return new OllamaClientTimesheetReader(
        RestClient.builder().baseUrl(baseUrl.toString()).requestFactory(requestFactory).build(), model);
  }

  @Override
  public ClientReportedHours read(ClientTimesheetDocument document) {
    if (!IMAGES.contains(document.format())) {
      throw new ClientTimesheetUnreadableException("Reading %s documents is not supported yet: %s".formatted(
          document.format(), document.fileName()));
    }
    var transcription = transcribe(document);
    try {
      return reportedHoursOf(transcription);
    } catch (IllegalArgumentException | DateTimeException e) {
      throw new ClientTimesheetUnreadableException(
          "Could not make sense of the transcription of %s: %s".formatted(document.fileName(), transcription), e);
    }
  }

  private Transcription transcribe(ClientTimesheetDocument document) {
    try {
      var answer = generate(new GenerateRequest(model, PROMPT,
          List.of(Base64.getEncoder().encodeToString(document.content())), false, TRANSCRIPTION_SCHEMA,
          Map.of("temperature", 0)));
      return jsonMapper.readValue(answer.response(), Transcription.class);
    } catch (RestClientException | JacksonException e) {
      throw new ClientTimesheetUnreadableException("Could not transcribe " + document.fileName(), e);
    }
  }

  private GenerateResponse generate(GenerateRequest request) {
    var response = restClient.post().uri("/api/generate")
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .body(GenerateResponse.class);
    if (response == null || response.response() == null) {
      throw new RestClientException("Ollama answered without a response");
    }
    return response;
  }

  /**
   * Dates the transcribed days, moving on to the next year when the days pass from December to January.
   *
   * @throws IllegalArgumentException when the transcription is incomplete or holds something other than hours
   * @throws DateTimeException when the transcription holds a day that does not exist
   */
  private static ClientReportedHours reportedHoursOf(Transcription transcription) {
    if (transcription.year() == null || transcription.days() == null) {
      throw new IllegalArgumentException("Incomplete transcription");
    }
    var hoursWorked = new LinkedHashMap<LocalDate, Hours>();
    var year = transcription.year();
    var previousMonth = 0;
    for (var day : transcription.days()) {
      if (day.month() < previousMonth) {
        year++;
      }
      previousMonth = day.month();
      hoursWorked.put(LocalDate.of(year, day.month(), day.dayOfMonth()), hoursOf(day.hoursWorked()));
    }
    return new ClientReportedHours(hoursWorked);
  }

  private static Hours hoursOf(String written) {
    var text = written == null ? "" : written.strip();
    return NO_HOURS.contains(text) ? Hours.ZERO : Hours.parse(text);
  }

  record GenerateRequest(String model, String prompt, List<String> images, boolean stream,
      Map<String, Object> format, Map<String, Object> options) {
  }

  record GenerateResponse(String response) {
  }

  record Transcription(Integer year, List<TranscribedDay> days) {
  }

  record TranscribedDay(int dayOfMonth, int month, String hoursWorked) {
  }
}
