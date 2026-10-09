package eu.openvalue.timesheet.adapter.out.persistence;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.TRAINING;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.PostgresTestConfiguration;
import eu.openvalue.timesheet.application.port.out.TimesheetChangedConcurrentlyException;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.HoursDifference;
import eu.openvalue.timesheet.domain.Kilometres;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Project;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against PostgreSQL with the schema from the migrations, each call in its own transaction as in the
 * application, so every timesheet found was really read back from the database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({PostgresTestConfiguration.class, JpaTimesheetsTest.Adapter.class})
class JpaTimesheetsTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final LocalDate TUESDAY = LocalDate.of(2026, 5, 5);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final Professional PIET = Professional.withEmail("piet.de.vries@openvalue.nl");
  private static final Assignment ACME = Assignment.at(new Client("ACME"), new Project("Platform"));

  @Autowired
  private JpaTimesheets timesheets;

  @Autowired
  private TimesheetEntityRepository repository;

  @BeforeEach
  void forgetAllTimesheets() {
    repository.deleteAll();
  }

  @Test
  void findsNoTimesheetThatWasNotKept() {
    assertThat(timesheets.find(JAN, MAY_2026)).isEmpty();
  }

  @Test
  void keepsWhatWasBookedOnEachDay() {
    // Given
    var timesheet = draft();
    timesheet.rebookDay(MONDAY, Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2)));
    timesheet.recordKilometres(MONDAY, Kilometres.of(42));
    timesheet.bookHours(TUESDAY, TRAINING, Hours.of(7, 30));
    timesheet.explain(TUESDAY, new Explanation("Java course"));

    // When
    timesheets.save(timesheet);

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow();
    assertThat(kept.days()).hasSize(31);
    assertThat(kept.day(MONDAY).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2)));
    assertThat(kept.day(MONDAY).kilometres()).isEqualTo(Kilometres.of(42));
    assertThat(kept.day(TUESDAY).hours(TRAINING)).isEqualTo(Hours.of(7, 30));
    assertThat(kept.day(TUESDAY).explanation()).contains(new Explanation("Java course"));
  }

  @Test
  void keepsTheChangesWhenSavedAgain() {
    // Given
    var timesheet = draft();
    timesheet.rebookDay(MONDAY, Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2)));
    timesheet.explain(MONDAY, new Explanation("Dentist"));
    timesheets.save(timesheet);
    var found = timesheets.find(JAN, MAY_2026).orElseThrow();

    // When
    found.rebookDay(MONDAY, Map.of(WORKED, Hours.of(8)));
    found.removeExplanation(MONDAY);
    timesheets.save(found);

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow().day(MONDAY);
    assertThat(kept.bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(8)));
    assertThat(kept.explanation()).isEmpty();
  }

  @Test
  void keepsOneTimesheetPerProfessionalAndMonth() {
    // Given
    var jansMay = draft();
    jansMay.bookHours(MONDAY, WORKED, Hours.of(8));
    timesheets.save(jansMay);
    timesheets.save(Timesheet.start(MAY_2026, PIET, Optional.empty()));
    timesheets.save(Timesheet.start(MAY_2026.plusMonths(1), JAN, Optional.of(ACME)));

    // When
    timesheets.save(timesheets.find(JAN, MAY_2026).orElseThrow());

    // Then
    assertThat(repository.count()).isEqualTo(3);
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(8));
    assertThat(timesheets.find(PIET, MAY_2026).orElseThrow().day(MONDAY).hours(WORKED)).isEqualTo(Hours.ZERO);
  }

  @Test
  void keepsTheAssignment() {
    // When
    timesheets.save(draft());

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().assignment()).contains(ACME);
  }

  @Test
  void keepsBeingOnTheBench() {
    // When
    timesheets.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().isOnBench()).isTrue();
  }

  @Test
  void keepsThatTheRegisteredAbsencesWereUnknown() {
    // Given
    var timesheet = Timesheet.restore(MAY_2026, JAN, Optional.of(ACME), draft().days(), true, Optional.empty(),
        Optional.empty(), Optional.empty(), Optional.empty(), false);

    // When
    timesheets.save(timesheet);

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().registeredAbsencesUnknown()).isTrue();
  }

  @Test
  void keepsASubmissionAndTheOneItCorrects() {
    // Given
    var timesheet = draft();
    timesheet.submit(LocalDate.of(2026, 6, 1));
    timesheet.reopenForCorrection();
    timesheet.submit(LocalDate.of(2026, 6, 3));

    // When
    timesheets.save(timesheet);

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow();
    assertThat(kept.submittedOn()).contains(LocalDate.of(2026, 6, 3));
    assertThat(kept.previouslySubmittedOn()).contains(LocalDate.of(2026, 6, 1));
  }

  @Test
  void keepsTheClientsTimesheetAndWhatWasReadFromIt() {
    // Given
    var timesheet = draft();
    timesheet.bookHours(MONDAY, WORKED, Hours.of(8));
    timesheet.attachClientTimesheet(clientTimesheet("acme-may.pdf"));
    timesheet.recordHoursReportedByClient(new ClientReportedHours(Map.of(
        MONDAY, Hours.of(7, 30), TUESDAY, Hours.ZERO)));

    // When
    timesheets.save(timesheet);

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow();
    assertThat(kept.clientTimesheet()).contains(clientTimesheet("acme-may.pdf"));
    assertThat(kept.hoursReportedByClient()).contains(new ClientReportedHours(Map.of(
        MONDAY, Hours.of(7, 30), TUESDAY, Hours.ZERO)));
    assertThat(kept.differencesWithClientTimesheet())
        .containsExactly(new HoursDifference(MONDAY, Hours.of(8), Hours.of(7, 30)));
  }

  @Test
  void keepsAClientsTimesheetWithoutDaysAsRead() {
    // Given
    var timesheet = draft();
    timesheet.attachClientTimesheet(clientTimesheet("empty.pdf"));
    timesheet.recordHoursReportedByClient(new ClientReportedHours(Map.of()));

    // When
    timesheets.save(timesheet);

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().hoursReportedByClient())
        .contains(new ClientReportedHours(Map.of()));
  }

  @Test
  void keepsThatTheClientsTimesheetCouldNotBeRead() {
    // Given
    var timesheet = draft();
    timesheet.attachClientTimesheet(clientTimesheet("scribbles.png"));
    timesheet.markClientTimesheetUnreadable();

    // When
    timesheets.save(timesheet);

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().isClientTimesheetUnreadable()).isTrue();
  }

  @Test
  void keepsAClientsTimesheetThatHasNotBeenReadAsUnread() {
    // Given
    var timesheet = draft();
    timesheet.attachClientTimesheet(clientTimesheet("acme-may.pdf"));

    // When
    timesheets.save(timesheet);

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow();
    assertThat(kept.hoursReportedByClient()).isEmpty();
    assertThat(kept.isClientTimesheetUnreadable()).isFalse();
  }

  @Test
  void keepsOnlyTheClientsTimesheetThatReplacedTheEarlierOne() {
    // Given
    var timesheet = draft();
    timesheet.attachClientTimesheet(clientTimesheet("first.pdf"));
    timesheet.recordHoursReportedByClient(new ClientReportedHours(Map.of(MONDAY, Hours.of(8))));
    timesheets.save(timesheet);
    var found = timesheets.find(JAN, MAY_2026).orElseThrow();

    // When
    found.attachClientTimesheet(clientTimesheet("second.pdf"));
    timesheets.save(found);

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow();
    assertThat(kept.clientTimesheet()).contains(clientTimesheet("second.pdf"));
    assertThat(kept.hoursReportedByClient()).isEmpty();
  }

  @Test
  void refusesToUndoAChangeKeptAfterTheTimesheetWasFound() {
    // Given
    timesheets.save(draft());
    var mine = timesheets.find(JAN, MAY_2026).orElseThrow();
    var theirs = timesheets.find(JAN, MAY_2026).orElseThrow();
    theirs.bookHours(TUESDAY, WORKED, Hours.of(6));
    timesheets.save(theirs);
    mine.bookHours(MONDAY, WORKED, Hours.of(4));

    // When / Then
    assertThatThrownBy(() -> timesheets.save(mine)).isInstanceOf(TimesheetChangedConcurrentlyException.class);
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().day(TUESDAY).hours(WORKED)).isEqualTo(Hours.of(6));
  }

  @Test
  void refusesASecondTimesheetStartedForTheSameProfessionalAndMonth() {
    // Given
    timesheets.save(draft());

    // When / Then
    assertThatThrownBy(() -> timesheets.save(draft())).isInstanceOf(TimesheetChangedConcurrentlyException.class);
  }

  @Test
  void keepsChangesToTheSameTimesheetOneAfterAnother() {
    // Given
    var timesheet = draft();
    timesheets.save(timesheet);
    timesheet.bookHours(MONDAY, WORKED, Hours.of(4));
    timesheets.save(timesheet);

    // When
    timesheet.bookHours(TUESDAY, WORKED, Hours.of(6));
    timesheets.save(timesheet);

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow();
    assertThat(kept.day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(4));
    assertThat(kept.day(TUESDAY).hours(WORKED)).isEqualTo(Hours.of(6));
  }

  @Test
  void keepsAChangeToADayAsAChangeToTheTimesheet() {
    // Given
    timesheets.save(draft());
    var mine = timesheets.find(JAN, MAY_2026).orElseThrow();
    var theirs = timesheets.find(JAN, MAY_2026).orElseThrow();
    theirs.recordKilometres(TUESDAY, Kilometres.of(12));
    timesheets.save(theirs);

    // When / Then
    assertThatThrownBy(() -> timesheets.save(mine)).isInstanceOf(TimesheetChangedConcurrentlyException.class);
  }

  private static Timesheet draft() {
    return Timesheet.start(MAY_2026, JAN, Optional.of(ACME));
  }

  private static ClientTimesheetDocument clientTimesheet(String fileName) {
    return new ClientTimesheetDocument(fileName, DocumentFormat.PDF, fileName.getBytes());
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Adapter {

    @Bean
    JpaTimesheets timesheets(TimesheetEntityRepository repository) {
      return new JpaTimesheets(repository);
    }
  }
}
