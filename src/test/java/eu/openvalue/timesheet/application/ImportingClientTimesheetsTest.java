package eu.openvalue.timesheet.application;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsCommand;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheet;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportingClientTimesheetsTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 5, 11);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final ClientTimesheetDocument WEEK_19 =
      new ClientTimesheetDocument("week-19.png", DocumentFormat.PNG, new byte[] {19});
  private static final ClientTimesheetDocument WEEK_20 =
      new ClientTimesheetDocument("week-20.png", DocumentFormat.PNG, new byte[] {20});
  private static final Map<ClientTimesheetDocument, ClientReportedHours> REPORTED = Map.of(
      WEEK_19, new ClientReportedHours(Map.of(MONDAY, Hours.of(8))),
      WEEK_20, new ClientReportedHours(Map.of(NEXT_MONDAY, Hours.of(7, 30))));

  private final FakeTimesheets timesheets = new FakeTimesheets();

  @BeforeEach
  void startDraft() {
    timesheets.save(Timesheet.start(MAY_2026, JAN, Optional.of(Assignment.at(new Client("ACME")))));
  }

  @Test
  void importsTheClientsTimesheetsAsRead() {
    // When
    importing(REPORTED::get).importClientTimesheets(command(WEEK_19, WEEK_20));

    // Then
    assertThat(draft().clientTimesheets().all()).containsExactly(
        ClientTimesheet.of(WEEK_19).readAs(REPORTED.get(WEEK_19)),
        ClientTimesheet.of(WEEK_20).readAs(REPORTED.get(WEEK_20)));
  }

  @Test
  void booksTheHoursWorkedTheyReport() {
    // When
    importing(REPORTED::get).importClientTimesheets(command(WEEK_19, WEEK_20));

    // Then
    assertThat(draft().day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(8));
    assertThat(draft().day(NEXT_MONDAY).hours(WORKED)).isEqualTo(Hours.of(7, 30));
  }

  @Test
  void importsAClientsTimesheetThatCannotBeReadAsUnreadable() {
    // Given
    ClientTimesheetReader week20Unreadable = document -> {
      if (document.equals(WEEK_20)) {
        throw new ClientTimesheetUnreadableException("Too blurry");
      }
      return REPORTED.get(document);
    };

    // When
    importing(week20Unreadable).importClientTimesheets(command(WEEK_19, WEEK_20));

    // Then
    assertThat(draft().clientTimesheets().unreadable()).containsExactly(ClientTimesheet.of(WEEK_20).unreadable());
    assertThat(draft().day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(8));
  }

  @Test
  void keepsAMonthNotKeptYetOnceClientsTimesheetsAreImported() {
    // Given
    var june = MAY_2026.plusMonths(1);

    // When
    importing(REPORTED::get).importClientTimesheets(new ImportClientTimesheetsCommand(JAN, june, List.of(WEEK_19)));

    // Then
    assertThat(timesheets.find(JAN, june).orElseThrow().clientTimesheets().documents()).containsExactly(WEEK_19);
  }

  @Test
  void readsNothingWhenTheTimesheetWasSubmitted() {
    // Given
    var submitted = draft();
    submitted.submit(LocalDate.of(2026, 6, 1));
    timesheets.save(submitted);
    var readings = new AtomicInteger();

    // When / Then
    assertThatThrownBy(() -> importing(document -> {
      readings.incrementAndGet();
      return REPORTED.get(document);
    }).importClientTimesheets(command(WEEK_19))).isInstanceOf(TimesheetAlreadySubmittedException.class);
    assertThat(readings).hasValue(0);
  }

  @Test
  void readsEachClientsTimesheetOnceWhenTheDraftIsChangedConcurrently() {
    // Given
    var readings = new AtomicInteger();
    timesheets.changeConcurrently(changed -> changed.bookHours(MONDAY.plusDays(1), LEAVE, Hours.of(8)));

    // When
    importing(document -> {
      readings.incrementAndGet();
      return REPORTED.get(document);
    }).importClientTimesheets(command(WEEK_19, WEEK_20));

    // Then
    assertThat(readings).hasValue(2);
    assertThat(draft().day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(8));
    assertThat(draft().day(MONDAY.plusDays(1)).hours(LEAVE)).isEqualTo(Hours.of(8));
  }

  private ImportingClientTimesheets importing(ClientTimesheetReader reader) {
    return new ImportingClientTimesheets(timesheets.current(), timesheets, reader);
  }

  private static ImportClientTimesheetsCommand command(ClientTimesheetDocument... documents) {
    return new ImportClientTimesheetsCommand(JAN, MAY_2026, List.of(documents));
  }

  private Timesheet draft() {
    return timesheets.find(JAN, MAY_2026).orElseThrow();
  }
}
