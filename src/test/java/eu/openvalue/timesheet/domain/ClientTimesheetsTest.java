package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientTimesheetsTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final LocalDate TUESDAY = LocalDate.of(2026, 5, 5);

  @Test
  void reportsTheHoursWorkedOfAllOfThem() {
    // Given
    var timesheets = ClientTimesheets.of(List.of(
        read("week-19.png", Map.of(MONDAY, Hours.of(8))), read("week-19b.png", Map.of(TUESDAY, Hours.of(7)))));

    // When / Then
    assertThat(timesheets.hoursWorkedIn(MAY_2026)).containsExactly(
        Map.entry(MONDAY, Hours.of(8)), Map.entry(TUESDAY, Hours.of(7)));
  }

  @Test
  void reportsTheHoursWorkedTheyAgreeOn() {
    // Given
    var timesheets = ClientTimesheets.of(List.of(
        read("week-19.png", Map.of(MONDAY, Hours.of(8))), read("week-19b.png", Map.of(MONDAY, Hours.of(8)))));

    // When / Then
    assertThat(timesheets.hoursWorkedIn(MAY_2026)).containsExactly(Map.entry(MONDAY, Hours.of(8)));
    assertThat(timesheets.daysInDisputeIn(MAY_2026)).isEmpty();
  }

  @Test
  void reportsNoHoursWorkedOnDaysTheyDisagreeAbout() {
    // Given
    var timesheets = ClientTimesheets.of(List.of(
        read("week-19.png", Map.of(MONDAY, Hours.of(8))), read("week-19b.png", Map.of(MONDAY, Hours.of(6)))));

    // When / Then
    assertThat(timesheets.hoursWorkedIn(MAY_2026)).isEmpty();
    assertThat(timesheets.daysInDisputeIn(MAY_2026)).containsExactly(MONDAY);
  }

  @Test
  void reportsOnlyTheDaysOfTheMonth() {
    // Given
    var timesheets = ClientTimesheets.of(List.of(
        read("week-18.png", Map.of(LocalDate.of(2026, 4, 30), Hours.of(8), LocalDate.of(2026, 5, 1), Hours.of(8)))));

    // When / Then
    assertThat(timesheets.hoursWorkedIn(MAY_2026)).containsOnlyKeys(LocalDate.of(2026, 5, 1));
  }

  @Test
  void coversTheDaysReportedOnByThoseThatWereRead() {
    // Given
    var timesheets = ClientTimesheets.of(List.of(read("week-19.png", Map.of(MONDAY, Hours.ZERO)),
        ClientTimesheet.of(document("blurry.png")).unreadable()));

    // When / Then
    assertThat(timesheets.cover(MONDAY)).isTrue();
    assertThat(timesheets.cover(TUESDAY)).isFalse();
  }

  @Test
  void listsThoseThatCouldNotBeRead() {
    // Given
    var blurry = ClientTimesheet.of(document("blurry.png")).unreadable();

    // When
    var timesheets = ClientTimesheets.of(List.of(read("week-19.png", Map.of()), blurry));

    // Then
    assertThat(timesheets.unreadable()).containsExactly(blurry);
  }

  @Test
  void listsThoseOfAnotherPeriod() {
    // Given
    var july = read("week-27.png", Map.of(LocalDate.of(2026, 7, 1), Hours.of(8)));

    // When
    var timesheets = ClientTimesheets.of(List.of(read("week-19.png", Map.of(MONDAY, Hours.of(8))), july));

    // Then
    assertThat(timesheets.ofAnotherPeriodThan(MAY_2026)).containsExactly(july);
  }

  @Test
  void cannotHoldTheSameDocumentTwice() {
    assertThatThrownBy(() -> ClientTimesheets.of(List.of(read("week-19.png", Map.of()), read("week-19.png", Map.of()))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void holdsNoneAtFirst() {
    assertThat(ClientTimesheets.NONE.isEmpty()).isTrue();
  }

  private static ClientTimesheet read(String fileName, Map<LocalDate, Hours> hoursWorked) {
    return ClientTimesheet.of(document(fileName)).readAs(new ClientReportedHours(hoursWorked));
  }

  private static ClientTimesheetDocument document(String fileName) {
    return new ClientTimesheetDocument(fileName, DocumentFormat.PNG, fileName.getBytes());
  }
}
