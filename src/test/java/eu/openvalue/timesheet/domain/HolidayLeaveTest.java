package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HolidayLeaveTest {

  private static final Year YEAR_2026 = Year.of(2026);
  private static final LocalDate LONG_AGO = LocalDate.of(2020, 1, 1);

  @ParameterizedTest
  @CsvSource({
      "2020-01-01, 208",
      "2026-01-01, 208",
      "2026-07-01, 105",
      "2026-12-31, 1",
      "2027-01-01, 0"
  })
  void entitlesToTheFullYearInProportionToTheDaysEmployed(LocalDate employedSince, int hours) {
    assertThat(HolidayLeave.of(YEAR_2026, employedSince, List.of()).entitlement()).isEqualTo(Hours.of(hours));
  }

  @Test
  void countsTheLeaveOfSubmittedTimesheetsAsTaken() {
    // When
    var leave = HolidayLeave.of(YEAR_2026, LONG_AGO, List.of(
        new BookedLeave(YearMonth.of(2026, 3), Hours.of(16), true),
        new BookedLeave(YearMonth.of(2026, 4), Hours.of(8), true)));

    // Then
    assertThat(leave.taken()).isEqualTo(Hours.of(24));
    assertThat(leave.reserved()).isEqualTo(Hours.ZERO);
  }

  @Test
  void countsTheLeaveOfTimesheetsNotSubmittedYetAsReserved() {
    // When
    var leave = HolidayLeave.of(YEAR_2026, LONG_AGO, List.of(
        new BookedLeave(YearMonth.of(2026, 5), Hours.of(4), false),
        new BookedLeave(YearMonth.of(2026, 8), Hours.of(40), false)));

    // Then
    assertThat(leave.reserved()).isEqualTo(Hours.of(44));
  }

  @Test
  void countsOnlyTheLeaveOfTheYear() {
    // When
    var leave = HolidayLeave.of(YEAR_2026, LONG_AGO, List.of(
        new BookedLeave(YearMonth.of(2025, 12), Hours.of(16), true),
        new BookedLeave(YearMonth.of(2027, 1), Hours.of(8), false)));

    // Then
    assertThat(leave.taken()).isEqualTo(Hours.ZERO);
    assertThat(leave.reserved()).isEqualTo(Hours.ZERO);
  }

  @Test
  void leavesWhatIsNeitherTakenNorReserved() {
    // When
    var leave = new HolidayLeave(YEAR_2026, Hours.of(208), Hours.of(40), Hours.of(16));

    // Then
    assertThat(leave.available()).isEqualTo(Hours.of(152));
    assertThat(leave.overdrawn()).isEqualTo(Hours.ZERO);
  }

  @Test
  void isOverdrawnWhenMoreIsTakenAndReservedThanEntitledTo() {
    // When
    var leave = new HolidayLeave(YEAR_2026, Hours.of(100), Hours.of(96), Hours.of(12));

    // Then
    assertThat(leave.available()).isEqualTo(Hours.ZERO);
    assertThat(leave.overdrawn()).isEqualTo(Hours.of(8));
  }
}
