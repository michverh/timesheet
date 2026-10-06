package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.OTHER;
import static eu.openvalue.timesheet.domain.HourCategory.TRAINING;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimesheetTest {

  private static final YearMonth FEBRUARY_2028 = YearMonth.of(2028, 2);
  private static final LocalDate A_DAY = LocalDate.of(2028, 2, 10);

  private final Timesheet timesheet = Timesheet.start(FEBRUARY_2028, new Professional("Jan Jansen"),
      Optional.of(Assignment.at(new Client("ACME"), new Project("Platform"))));

  @Test
  void startsWithAnEmptyEntryForEveryDayOfTheMonth() {
    assertThat(timesheet.days())
        .extracting(DayEntry::date)
        .containsExactlyElementsOf(LocalDate.of(2028, 2, 1).datesUntil(LocalDate.of(2028, 3, 1)).toList())
        .hasSize(29);
  }

  @Test
  void isOnTheBenchWithoutAnAssignment() {
    // When
    var onBench = Timesheet.start(FEBRUARY_2028, new Professional("Jan Jansen"), Optional.empty());

    // Then
    assertThat(onBench.isOnBench()).isTrue();
  }

  @Test
  void booksHoursInSeveralCategoriesOnOneDay() {
    // When
    timesheet.bookHours(A_DAY, WORKED, Hours.of(4));
    timesheet.bookHours(A_DAY, LEAVE, Hours.of(4));

    // Then
    assertThat(timesheet.day(A_DAY).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(4), LEAVE, Hours.of(4)));
  }

  @Test
  void replacesPreviouslyBookedHoursInTheSameCategory() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(8));

    // When
    timesheet.bookHours(A_DAY, WORKED, Hours.of(6));

    // Then
    assertThat(timesheet.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(6));
  }

  @Test
  void clearsACategoryWhenBookingZeroHours() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(8));

    // When
    timesheet.bookHours(A_DAY, WORKED, Hours.ZERO);

    // Then
    assertThat(timesheet.day(A_DAY).bookedHours()).isEmpty();
  }

  @Test
  void allowsBookingUpToTwentyFourHoursOnADay() {
    // When
    timesheet.bookHours(A_DAY, WORKED, Hours.of(16));
    timesheet.bookHours(A_DAY, HourCategory.OVERTIME, Hours.of(8));

    // Then
    assertThat(timesheet.day(A_DAY).totalHours()).isEqualTo(Hours.of(24));
  }

  @Test
  void rejectsBookingMoreThanTwentyFourHoursOnADay() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(20));

    // When / Then
    assertThatThrownBy(() -> timesheet.bookHours(A_DAY, HourCategory.OVERTIME, Hours.of(5)))
        .isInstanceOf(DailyHoursLimitExceededException.class);
    assertThat(timesheet.day(A_DAY).totalHours()).isEqualTo(Hours.of(20));
  }

  @Test
  void rejectsDaysOutsideTheMonth() {
    assertThatThrownBy(() -> timesheet.bookHours(LocalDate.of(2028, 3, 1), WORKED, Hours.of(8)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recordsKilometresAndExplanationPerDay() {
    // When
    timesheet.recordKilometres(A_DAY, Kilometres.of(120));
    timesheet.explain(A_DAY, new Explanation("Visit to client office"));

    // Then
    var day = timesheet.day(A_DAY);
    assertThat(day.kilometres()).isEqualTo(Kilometres.of(120));
    assertThat(day.explanation()).contains(new Explanation("Visit to client office"));
  }

  @Test
  void removesAnExplanation() {
    // Given
    timesheet.explain(A_DAY, new Explanation("Visit to client office"));

    // When
    timesheet.removeExplanation(A_DAY);

    // Then
    assertThat(timesheet.day(A_DAY).explanation()).isEmpty();
  }

  @Test
  void reportsTrainingAndOtherHoursWithoutExplanation() {
    // Given
    var training = LocalDate.of(2028, 2, 1);
    var other = LocalDate.of(2028, 2, 2);
    var explainedTraining = LocalDate.of(2028, 2, 3);
    timesheet.bookHours(training, TRAINING, Hours.of(8));
    timesheet.bookHours(other, OTHER, Hours.of(2));
    timesheet.bookHours(explainedTraining, TRAINING, Hours.of(8));
    timesheet.explain(explainedTraining, new Explanation("Java conference"));
    timesheet.bookHours(LocalDate.of(2028, 2, 4), WORKED, Hours.of(8));

    // When
    var lacking = timesheet.daysLackingRequiredExplanation();

    // Then
    assertThat(lacking).containsExactly(training, other);
  }

  @Test
  void canBeSubmittedDuringItsOwnMonth() {
    // When
    timesheet.submit(LocalDate.of(2028, 2, 15));

    // Then
    assertThat(timesheet.submittedOn()).contains(LocalDate.of(2028, 2, 15));
  }

  @Test
  void canBeSubmittedAfterItsMonth() {
    // When
    timesheet.submit(LocalDate.of(2028, 4, 1));

    // Then
    assertThat(timesheet.isSubmitted()).isTrue();
  }

  @Test
  void cannotBeSubmittedBeforeItsMonthHasStarted() {
    assertThatThrownBy(() -> timesheet.submit(LocalDate.of(2028, 1, 31)))
        .isInstanceOf(FutureMonthNotSubmittableException.class);
  }

  @Test
  void cannotBeChangedOnceSubmitted() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));

    // When / Then
    assertThatThrownBy(() -> timesheet.bookHours(A_DAY, WORKED, Hours.of(8)))
        .isInstanceOf(TimesheetAlreadySubmittedException.class);
  }

  @Test
  void cannotBeSubmittedTwice() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));

    // When / Then
    assertThatThrownBy(() -> timesheet.submit(LocalDate.of(2028, 3, 2)))
        .isInstanceOf(TimesheetAlreadySubmittedException.class);
  }
}
