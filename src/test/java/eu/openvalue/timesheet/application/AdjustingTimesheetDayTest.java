package eu.openvalue.timesheet.application;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayCommand;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.domain.DailyHoursLimitExceededException;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdjustingTimesheetDayTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");

  private final FakeTimesheets timesheets = new FakeTimesheets();
  private final AdjustingTimesheetDay adjustDay = new AdjustingTimesheetDay(timesheets);

  private Timesheet draft;

  @BeforeEach
  void startDraft() {
    draft = Timesheet.start(MAY_2026, JAN, Optional.empty());
    draft.bookHours(MONDAY, WORKED, Hours.of(8));
    draft.explain(MONDAY, new Explanation("Old explanation"));
    timesheets.save(draft);
  }

  @Test
  void replacesTheHoursOfTheDay() {
    // When
    adjustDay.adjustDay(command(Map.of(WORKED, Hours.of(4), LEAVE, Hours.of(4)), Kilometres.ZERO, Optional.empty()));

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().day(MONDAY).bookedHours())
        .isEqualTo(Map.of(WORKED, Hours.of(4), LEAVE, Hours.of(4)));
  }

  @Test
  void recordsKilometresAndExplanation() {
    // When
    var adjusted = adjustDay.adjustDay(
        command(Map.of(WORKED, Hours.of(8)), Kilometres.of(120), Optional.of(new Explanation("Client visit"))));

    // Then
    assertThat(adjusted.day(MONDAY).kilometres()).isEqualTo(Kilometres.of(120));
    assertThat(adjusted.day(MONDAY).explanation()).contains(new Explanation("Client visit"));
  }

  @Test
  void removesTheExplanationWhenNoneIsGiven() {
    // When
    var adjusted = adjustDay.adjustDay(command(Map.of(WORKED, Hours.of(8)), Kilometres.ZERO, Optional.empty()));

    // Then
    assertThat(adjusted.day(MONDAY).explanation()).isEmpty();
  }

  @Test
  void leavesTheDayUntouchedWhenTheAdjustmentBreaksARule() {
    // When / Then
    assertThatThrownBy(() -> adjustDay.adjustDay(
        command(Map.of(WORKED, Hours.of(25)), Kilometres.of(50), Optional.of(new Explanation("Too much")))))
        .isInstanceOf(DailyHoursLimitExceededException.class);
    var day = timesheets.find(JAN, MAY_2026).orElseThrow().day(MONDAY);
    assertThat(day.bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(8)));
    assertThat(day.kilometres()).isEqualTo(Kilometres.ZERO);
    assertThat(day.explanation()).contains(new Explanation("Old explanation"));
  }

  @Test
  void requiresADraftForTheMonth() {
    // Given
    var dayInJune = LocalDate.of(2026, 6, 1);

    // When / Then
    assertThatThrownBy(() -> adjustDay.adjustDay(new AdjustTimesheetDayCommand(JAN, dayInJune,
        Map.of(WORKED, Hours.of(8)), Kilometres.ZERO, Optional.empty())))
        .isInstanceOf(TimesheetDraftNotFoundException.class);
  }

  private static AdjustTimesheetDayCommand command(Map<HourCategory, Hours> hours,
      Kilometres kilometres, Optional<Explanation> explanation) {
    return new AdjustTimesheetDayCommand(JAN, MONDAY, hours, kilometres, explanation);
  }

  @Test
  void keepsWhatWasChangedConcurrentlyOnAnotherDay() {
    // Given
    var tuesday = MONDAY.plusDays(1);
    timesheets.changeConcurrently(changed -> changed.bookHours(tuesday, WORKED, Hours.of(6)));

    // When
    adjustDay.adjustDay(command(Map.of(WORKED, Hours.of(4)), Kilometres.ZERO, Optional.empty()));

    // Then
    var kept = timesheets.find(JAN, MAY_2026).orElseThrow();
    assertThat(kept.day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(4));
    assertThat(kept.day(tuesday).hours(WORKED)).isEqualTo(Hours.of(6));
  }
}
