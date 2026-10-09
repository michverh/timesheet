package eu.openvalue.timesheet.adapter.out.reading;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads client timesheets by having a language model transcribe them: screenshots as images, PDF documents as images
 * of their pages, and Excel workbooks written out as text. The model only transcribes what the timesheet says, in a
 * fixed structure; turning that into dates and hours is done here, whichever model transcribes.
 */
public final class TranscribingClientTimesheetReader implements ClientTimesheetReader {

  private static final Set<String> NO_HOURS = Set.of("", "-", "–");

  /**
   * Readable for a vision model, yet small enough not to slow it down: higher resolutions read worse, not better.
   */
  private static final float PDF_DOTS_PER_INCH = 100;

  private static final String IMAGE_INTRODUCTION = """
      This image is a timesheet in which a client registered the hours a consultant worked.""";

  private static final String PAGES_INTRODUCTION = """
      These images are the pages of a timesheet in which a client registered the hours a consultant worked.""";

  private static final String SPREADSHEET_INTRODUCTION = """
      Below the instructions is a spreadsheet in which a client registered the hours a consultant worked, written \
      out as a Markdown table per sheet, headed by the column letters and with the row numbers in front.""";

  /**
   * Deliberately without example dates or months: small models copy them instead of reading the timesheet.
   */
  private static final String INSTRUCTIONS = """
      List every day that has a row or column in the timesheet, in order, including days without hours.
      For each day give its day of the month (d), its month number (m) and the hours worked exactly as written (h).
      Use an empty string for h when no hours worked are written for that day.
      Also list every total of hours the timesheet states, exactly as written (totals): when a row or column of totals
      holds a total for each kind of hours, list each of those numbers. List none when the timesheet states no totals.
      Do not add up anything yourself.
      Only take the regular hours worked, not overtime, sickness, leave, public holidays, training, other hours or
      kilometres.
      When the days only show their day number, take the month and year from where the timesheet states them, for example
      next to "Maand" and "Jaar", or "Month" and "Year".""";

  /**
   * Short property names keep the answer short, which is what a local model spends most of its time on. The
   * properties are ordered, because models answer in the order of the schema: first the year, then per day the date
   * before its hours, and the totals last.
   */
  private static final Map<String, Object> TRANSCRIPTION_SCHEMA = ordered(
      "type", "object",
      "properties", ordered(
          "year", ordered("type", "integer", "description", "the year the timesheet concerns"),
          "days", ordered("type", "array", "items", ordered(
              "type", "object",
              "properties", ordered(
                  "d", ordered("type", "integer", "description", "day of the month, 1 to 31"),
                  "m", ordered("type", "integer", "description", "month number of the day, 1 to 12"),
                  "h", ordered("type", "string",
                      "description", "the hours worked on the day exactly as written, or empty")),
              "required", List.of("d", "m", "h"))),
          "totals", ordered("type", "array", "items", ordered("type", "string"),
              "description", "every total of hours the timesheet states, exactly as written")),
      "required", List.of("year", "days", "totals"));

  private final Transcriber transcriber;
  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  public TranscribingClientTimesheetReader(Transcriber transcriber) {
    this.transcriber = Objects.requireNonNull(transcriber, "transcriber");
  }

  @Override
  public ClientReportedHours read(ClientTimesheetDocument document) {
    var transcription = transcriptionOf(document, transcriber.transcribe(requestFor(document)));
    var reported = reportedHoursOf(document, transcription);
    if (!addsUpToAStatedTotal(reported, transcription)) {
      throw new ClientTimesheetUnreadableException(
          "The hours read from %s add up to %s, which matches none of the totals it states: %s".formatted(
              document.fileName(), reported.total(), transcription.totals()));
    }
    return reported;
  }

  private static ClientReportedHours reportedHoursOf(ClientTimesheetDocument document, Transcription transcription) {
    try {
      return reportedHoursOf(transcription);
    } catch (IllegalArgumentException | DateTimeException e) {
      throw new ClientTimesheetUnreadableException(
          "Could not make sense of the transcription of %s: %s".formatted(document.fileName(), transcription), e);
    }
  }

  /**
   * Guards against misread days: when the timesheet states totals, the hours read must add up to one of them. Which
   * one is left open, because timesheets often state several, such as a grand total of all kinds of hours.
   */
  private static boolean addsUpToAStatedTotal(ClientReportedHours reported, Transcription transcription) {
    var statedTotals = transcription.totals() == null ? List.<String>of() : transcription.totals();
    var readableTotals = statedTotals.stream().flatMap(total -> hoursIn(total).stream()).toList();
    return readableTotals.isEmpty() || readableTotals.contains(reported.total());
  }

  private static Optional<Hours> hoursIn(String total) {
    try {
      return Optional.of(Hours.parse(total));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static TranscriptionRequest requestFor(ClientTimesheetDocument document) {
    return switch (document.format()) {
      case PNG, JPEG -> new TranscriptionRequest(IMAGE_INTRODUCTION + "\n" + INSTRUCTIONS,
          List.of(new TranscriptionRequest.Image(document.format().mediaType(), document.content())),
          TRANSCRIPTION_SCHEMA);
      case XLSX -> new TranscriptionRequest(
          SPREADSHEET_INTRODUCTION + "\n" + INSTRUCTIONS + "\n\n" + spreadsheetTranscriptOf(document), List.of(),
          TRANSCRIPTION_SCHEMA);
      case PDF -> pagesRequestFor(document);
    };
  }

  private static TranscriptionRequest pagesRequestFor(ClientTimesheetDocument document) {
    var pages = pdfPagesOf(document).stream()
        .map(page -> new TranscriptionRequest.Image(DocumentFormat.PNG.mediaType(), page))
        .toList();
    var introduction = pages.size() == 1 ? IMAGE_INTRODUCTION : PAGES_INTRODUCTION;
    return new TranscriptionRequest(introduction + "\n" + INSTRUCTIONS, pages, TRANSCRIPTION_SCHEMA);
  }

  private static List<byte[]> pdfPagesOf(ClientTimesheetDocument document) {
    try {
      var pages = PdfPages.asPngImages(document.content(), PDF_DOTS_PER_INCH);
      if (pages.isEmpty()) {
        throw new ClientTimesheetUnreadableException("A PDF document without pages: " + document.fileName());
      }
      return pages;
    } catch (IOException e) {
      throw new ClientTimesheetUnreadableException("Not a readable PDF document: " + document.fileName(), e);
    }
  }

  private static String spreadsheetTranscriptOf(ClientTimesheetDocument document) {
    try {
      return SpreadsheetTranscript.of(document.content());
    } catch (IOException | RuntimeException e) {
      throw new ClientTimesheetUnreadableException("Not a readable Excel workbook: " + document.fileName(), e);
    }
  }

  private Transcription transcriptionOf(ClientTimesheetDocument document, String json) {
    try {
      return jsonMapper.readValue(json, Transcription.class);
    } catch (JacksonException e) {
      throw new ClientTimesheetUnreadableException(
          "The transcription of %s is not in the expected structure: %s".formatted(document.fileName(), json), e);
    }
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

  /**
   * A map that keeps its entries in the order given, unlike {@link Map#of}.
   */
  private static Map<String, Object> ordered(Object... keysAndValues) {
    var map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      map.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return Collections.unmodifiableMap(map);
  }

  record Transcription(Integer year, List<TranscribedDay> days, List<String> totals) {
  }

  record TranscribedDay(@JsonProperty("d") int dayOfMonth, @JsonProperty("m") int month,
      @JsonProperty("h") String hoursWorked) {
  }
}
