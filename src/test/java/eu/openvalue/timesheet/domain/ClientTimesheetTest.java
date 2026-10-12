package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientTimesheetTest {

  private static final YearMonth JULY_2026 = YearMonth.of(2026, 7);
  private static final LocalDate JULY_1 = LocalDate.of(2026, 7, 1);
  private static final ClientTimesheetDocument DOCUMENT =
      new ClientTimesheetDocument("acme-july.pdf", DocumentFormat.PDF, new byte[] {1, 2, 3});
  private static final ClientReportedHours REPORTED = new ClientReportedHours(Map.of(JULY_1, Hours.of(7)));

  private final ClientTimesheet uploaded = ClientTimesheet.of(DOCUMENT);

  @Test
  void hasNotBeenReadWhenUploaded() {
    assertThat(uploaded.hoursReportedByClient()).isEmpty();
    assertThat(uploaded.isUnreadable()).isFalse();
  }

  @Test
  void givesTheHoursWorkedOnceRead() {
    assertThat(uploaded.readAs(REPORTED).hoursReportedByClient()).contains(REPORTED);
  }

  @Test
  void hasNoHoursWorkedWhenUnreadable() {
    // When
    var unreadable = uploaded.readAs(REPORTED).unreadable();

    // Then
    assertThat(unreadable.isUnreadable()).isTrue();
    assertThat(unreadable.hoursReportedByClient()).isEmpty();
  }

  @Test
  void isNoLongerUnreadableOnceRead() {
    assertThat(uploaded.unreadable().readAs(REPORTED).isUnreadable()).isFalse();
  }

  @Test
  void keepsTheDocumentWhateverWasRead() {
    assertThat(uploaded.readAs(REPORTED).document()).isEqualTo(DOCUMENT);
  }

  @Test
  void concernsAnotherPeriodWhenItCoversNoDayOfTheMonth() {
    assertThat(uploaded.readAs(REPORTED).isOfAnotherPeriodThan(YearMonth.of(2026, 10))).isTrue();
  }

  @Test
  void concernsTheMonthWhenItCoversOneOfItsDays() {
    assertThat(uploaded.readAs(REPORTED).isOfAnotherPeriodThan(JULY_2026)).isFalse();
  }

  @Test
  void concernsNoOtherPeriodBeforeItIsRead() {
    assertThat(uploaded.isOfAnotherPeriodThan(YearMonth.of(2026, 10))).isFalse();
  }

  @Test
  void equalsAClientTimesheetWithTheSameDocumentAndReading() {
    assertThat(uploaded.readAs(REPORTED)).isEqualTo(ClientTimesheet.of(DOCUMENT).readAs(REPORTED));
  }
}
