package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientReportedHoursTest {

  private static final LocalDate JULY_1 = LocalDate.of(2026, 7, 1);
  private static final LocalDate JULY_2 = LocalDate.of(2026, 7, 2);
  private static final LocalDate JULY_3 = LocalDate.of(2026, 7, 3);

  @Test
  void givesTheHoursWorkedOnTheDaysItCovers() {
    // Given
    var reported = new ClientReportedHours(Map.of(JULY_2, Hours.of(8)));

    // When / Then
    assertThat(reported.on(JULY_2)).contains(Hours.of(8));
    assertThat(reported.on(JULY_3)).isEmpty();
  }

  @Test
  void totalsTheHoursWorkedOnAllDaysItCovers() {
    // Given
    var reported = new ClientReportedHours(Map.of(
        LocalDate.of(2026, 6, 30), Hours.of(8),
        JULY_1, Hours.ZERO,
        JULY_2, Hours.of(7, 30)));

    // When / Then
    assertThat(reported.total()).isEqualTo(Hours.of(15, 30));
  }

  @Test
  void cannotBeChangedThroughTheHoursItWasGiven() {
    // Given
    var hours = new HashMap<LocalDate, Hours>();
    hours.put(JULY_2, Hours.of(8));
    var reported = new ClientReportedHours(hours);

    // When
    hours.put(JULY_3, Hours.of(8));

    // Then
    assertThat(reported.on(JULY_3)).isEmpty();
  }

  @Test
  void differsOnTheCoveredDaysWhereTheHoursWorkedDiffer() {
    // Given
    var days = MonthOfDays.emptyFor(YearMonth.of(2026, 7))
        .with(DayEntry.empty(JULY_1).withHours(WORKED, Hours.of(8)))
        .with(DayEntry.empty(JULY_2).withHours(WORKED, Hours.of(8)))
        .with(DayEntry.empty(JULY_3).withHours(WORKED, Hours.of(8)));
    var reported = new ClientReportedHours(Map.of(
        JULY_1, Hours.ZERO,
        JULY_2, Hours.of(8),
        JULY_3, Hours.of(7, 30)));

    // When / Then
    assertThat(reported.differencesWith(days)).containsExactly(
        new HoursDifference(JULY_1, Hours.of(8), Hours.ZERO),
        new HoursDifference(JULY_3, Hours.of(8), Hours.of(7, 30)));
  }

  @Test
  void leavesDaysOutsideTheMonthOutOfTheComparison() {
    // Given
    var days = MonthOfDays.emptyFor(YearMonth.of(2026, 7));
    var reported = new ClientReportedHours(Map.of(LocalDate.of(2026, 6, 30), Hours.of(8)));

    // When / Then
    assertThat(reported.differencesWith(days)).isEmpty();
  }

  @Test
  void leavesDaysItDoesNotCoverOutOfTheComparison() {
    // Given
    var days = MonthOfDays.emptyFor(YearMonth.of(2026, 7))
        .with(DayEntry.empty(JULY_3).withHours(WORKED, Hours.of(8)));
    var reported = new ClientReportedHours(Map.of(JULY_2, Hours.ZERO));

    // When / Then
    assertThat(reported.differencesWith(days)).isEmpty();
  }

  @Test
  void startsOnTheFirstDayItCovers() {
    var reported = new ClientReportedHours(Map.of(JULY_3, Hours.of(8), JULY_1, Hours.of(8), JULY_2, Hours.ZERO));

    assertThat(reported.firstDay()).contains(JULY_1);
  }

  @Test
  void endsOnTheLastDayItCovers() {
    var reported = new ClientReportedHours(Map.of(JULY_3, Hours.of(8), JULY_1, Hours.of(8), JULY_2, Hours.ZERO));

    assertThat(reported.lastDay()).contains(JULY_3);
  }

  @Test
  void coversNoPeriodWhenItCoversNoDays() {
    assertThat(new ClientReportedHours(Map.of()).firstDay()).isEmpty();
  }

  @Test
  void coversAMonthWhenItCoversAtLeastOneOfItsDays() {
    var reported = new ClientReportedHours(Map.of(LocalDate.of(2026, 6, 30), Hours.of(8), JULY_1, Hours.of(8)));

    assertThat(reported.coversAnyDayOf(YearMonth.of(2026, 7))).isTrue();
  }

  @Test
  void doesNotCoverAMonthWhenItCoversNoneOfItsDays() {
    var reported = new ClientReportedHours(Map.of(JULY_1, Hours.of(8)));

    assertThat(reported.coversAnyDayOf(YearMonth.of(2026, 10))).isFalse();
  }

  @Test
  void givesTheHoursWorkedPerDayInOrder() {
    var reported = new ClientReportedHours(Map.of(JULY_3, Hours.of(8), JULY_1, Hours.of(7, 30)));

    assertThat(reported.hoursWorkedPerDay()).containsExactly(
        Map.entry(JULY_1, Hours.of(7, 30)), Map.entry(JULY_3, Hours.of(8)));
  }
}
