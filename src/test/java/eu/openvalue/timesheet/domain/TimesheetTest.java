package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.OTHER;
import static eu.openvalue.timesheet.domain.HourCategory.OVERTIME;
import static eu.openvalue.timesheet.domain.HourCategory.STAND_BY;
import static eu.openvalue.timesheet.domain.HourCategory.TRAINING;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimesheetTest {

  private static final YearMonth FEBRUARY_2028 = YearMonth.of(2028, 2);
  private static final LocalDate A_DAY = LocalDate.of(2028, 2, 10);
  private static final LocalDate MONDAY_7 = LocalDate.of(2028, 2, 7);
  private static final LocalDate FIRST_SUBMISSION = LocalDate.of(2028, 3, 1);
  private static final LocalDate SUBMISSION = LocalDate.of(2028, 3, 4);
  private static final Professional PROFESSIONAL = Professional.withEmail("jan.jansen@openvalue.nl");

  private final Timesheet timesheet = Timesheet.start(FEBRUARY_2028, PROFESSIONAL,
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
    var onBench = Timesheet.start(FEBRUARY_2028, PROFESSIONAL, Optional.empty());

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
  void booksHoursAndMinutes() {
    // When
    timesheet.bookHours(A_DAY, WORKED, Hours.of(7, 41));

    // Then
    assertThat(timesheet.day(A_DAY).totalHours()).isEqualTo(Hours.of(7, 41));
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

  @Test
  void rebooksAllHoursOfADayAtOnce() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(8));

    // When
    timesheet.rebookDay(A_DAY, Map.of(LEAVE, Hours.of(6), TRAINING, Hours.of(2)));

    // Then
    assertThat(timesheet.day(A_DAY).bookedHours()).isEqualTo(Map.of(LEAVE, Hours.of(6), TRAINING, Hours.of(2)));
  }

  @Test
  void movesAFullDayBetweenCategoriesWhenRebooking() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(20));

    // When
    timesheet.rebookDay(A_DAY, Map.of(WORKED, Hours.ZERO, LEAVE, Hours.of(20)));

    // Then
    assertThat(timesheet.day(A_DAY).bookedHours()).isEqualTo(Map.of(LEAVE, Hours.of(20)));
  }

  @Test
  void keepsTheDayUnchangedWhenRebookingMoreThanTwentyFourHours() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(8));

    // When / Then
    assertThatThrownBy(() -> timesheet.rebookDay(A_DAY, Map.of(WORKED, Hours.of(20), OTHER, Hours.of(5))))
        .isInstanceOf(DailyHoursLimitExceededException.class);
    assertThat(timesheet.day(A_DAY).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(8)));
  }

  @Test
  void totalsTheHoursPerCategoryOverTheMonthCarryingMinutesIntoHours() {
    // Given
    timesheet.bookHours(LocalDate.of(2028, 2, 1), WORKED, Hours.of(7, 40));
    timesheet.bookHours(LocalDate.of(2028, 2, 2), WORKED, Hours.of(7, 35));
    timesheet.bookHours(LocalDate.of(2028, 2, 2), LEAVE, Hours.of(0, 25));

    // When / Then
    assertThat(timesheet.totalHours(WORKED)).isEqualTo(Hours.of(15, 15));
  }

  @Test
  void totalsTheKilometresOverTheMonth() {
    // Given
    timesheet.recordKilometres(LocalDate.of(2028, 2, 1), Kilometres.of(42));
    timesheet.recordKilometres(LocalDate.of(2028, 2, 3), Kilometres.of(58));

    // When / Then
    assertThat(timesheet.totalKilometres()).isEqualTo(Kilometres.of(100));
  }

  @Test
  void canBeCheckedForSubmissionWithoutBeingSubmitted() {
    // When
    timesheet.ensureSubmittableOn(LocalDate.of(2028, 2, 15));

    // Then
    assertThat(timesheet.isSubmitted()).isFalse();
  }

  @Test
  void failsTheSubmissionCheckBeforeItsMonthHasStarted() {
    assertThatThrownBy(() -> timesheet.ensureSubmittableOn(LocalDate.of(2028, 1, 31)))
        .isInstanceOf(FutureMonthNotSubmittableException.class);
  }

  @Test
  void canBeChangedAgainOnceReopenedForCorrection() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));

    // When
    timesheet.reopenForCorrection();
    timesheet.bookHours(A_DAY, WORKED, Hours.of(6));

    // Then
    assertThat(timesheet.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(6));
  }

  @Test
  void remembersWhenItWasSubmittedBeforeTheCorrection() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));

    // When
    timesheet.reopenForCorrection();

    // Then
    assertThat(timesheet.isSubmitted()).isFalse();
    assertThat(timesheet.previouslySubmittedOn()).contains(LocalDate.of(2028, 3, 1));
  }

  @Test
  void canBeSubmittedAgainAfterACorrection() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));
    timesheet.reopenForCorrection();

    // When
    timesheet.submit(LocalDate.of(2028, 3, 5));

    // Then
    assertThat(timesheet.submittedOn()).contains(LocalDate.of(2028, 3, 5));
    assertThat(timesheet.previouslySubmittedOn()).contains(LocalDate.of(2028, 3, 1));
  }

  @Test
  void cannotBeReopenedWhenNotSubmitted() {
    assertThatThrownBy(timesheet::reopenForCorrection).isInstanceOf(TimesheetNotSubmittedException.class);
  }

  @Test
  void restartsFromAFreshlyPreparedTimesheet() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(6));
    var prepared = Timesheet.start(FEBRUARY_2028, PROFESSIONAL, Optional.empty());
    prepared.bookHours(A_DAY, LEAVE, Hours.of(8));

    // When
    timesheet.restartFrom(prepared);

    // Then
    assertThat(timesheet.day(A_DAY).bookedHours()).isEqualTo(Map.of(LEAVE, Hours.of(8)));
    assertThat(timesheet.isOnBench()).isTrue();
  }

  @Test
  void remainsACorrectionWhenRestarted() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));
    timesheet.reopenForCorrection();

    // When
    timesheet.restartFrom(Timesheet.start(FEBRUARY_2028, PROFESSIONAL, Optional.empty()));

    // Then
    assertThat(timesheet.previouslySubmittedOn()).contains(LocalDate.of(2028, 3, 1));
  }

  @Test
  void cannotRestartOnceSubmitted() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));

    // When / Then
    assertThatThrownBy(() -> timesheet.restartFrom(Timesheet.start(FEBRUARY_2028, PROFESSIONAL, Optional.empty())))
        .isInstanceOf(TimesheetAlreadySubmittedException.class);
  }

  @Test
  void canOnlyRestartFromATimesheetOfTheSameProfessionalAndMonth() {
    assertThatThrownBy(() -> timesheet.restartFrom(
        Timesheet.start(FEBRUARY_2028.plusMonths(1), PROFESSIONAL, Optional.empty())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void holdsTheClientsTimesheets() {
    // Given
    var firstWeek = read("week-6.png", Map.of(LocalDate.of(2028, 2, 7), Hours.of(8)));
    var secondWeek = read("week-7.png", Map.of(A_DAY, Hours.of(8)));

    // When
    importAll(firstWeek, secondWeek);

    // Then
    assertThat(timesheet.clientTimesheets().all()).containsExactly(firstWeek, secondWeek);
  }

  @Test
  void replacesTheClientsTimesheetsImportedBefore() {
    // Given
    importAll(read("first.pdf", Map.of()));

    // When
    importAll(read("second.pdf", Map.of()));

    // Then
    assertThat(timesheet.clientTimesheets().documents()).extracting(ClientTimesheetDocument::fileName)
        .containsExactly("second.pdf");
  }

  @Test
  void refusesClientsTimesheetsOnceSubmitted() {
    // Given
    timesheet.submit(LocalDate.of(2028, 3, 1));

    // When / Then
    assertThatThrownBy(() -> importAll(read("late.pdf", Map.of())))
        .isInstanceOf(TimesheetAlreadySubmittedException.class);
  }

  @Test
  void lacksTheClientsTimesheetsWhenWorkingForAClientWithoutThem() {
    assertThat(timesheet.lacksClientTimesheet()).isTrue();
  }

  @Test
  void doesNotLackTheClientsTimesheetsOnceImported() {
    // When
    importAll(read("acme-february.pdf", Map.of()));

    // Then
    assertThat(timesheet.lacksClientTimesheet()).isFalse();
  }

  @Test
  void doesNotLackClientsTimesheetsOnTheBench() {
    assertThat(Timesheet.start(FEBRUARY_2028, PROFESSIONAL, Optional.empty()).lacksClientTimesheet()).isFalse();
  }

  @Test
  void booksTheHoursWorkedThatTheClientsTimesheetsReport() {
    // When
    importAll(read("week-6.png", Map.of(LocalDate.of(2028, 2, 7), Hours.of(8))),
        read("week-7.png", Map.of(A_DAY, Hours.of(7, 30))));

    // Then
    assertThat(timesheet.day(LocalDate.of(2028, 2, 7)).hours(WORKED)).isEqualTo(Hours.of(8));
    assertThat(timesheet.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(7, 30));
  }

  @Test
  void replacesTheHoursWorkedButKeepsTheOtherHoursOfTheDay() {
    // Given
    timesheet.rebookDay(A_DAY, Map.of(WORKED, Hours.of(8), LEAVE, Hours.of(4)));

    // When
    importAll(read("week-7.png", Map.of(A_DAY, Hours.of(4))));

    // Then
    assertThat(timesheet.day(A_DAY).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(4), LEAVE, Hours.of(4)));
  }

  @Test
  void leavesDaysTheClientsTimesheetsDisagreeAboutAsTheyAre() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(6));

    // When
    importAll(read("week-7.png", Map.of(A_DAY, Hours.of(8))), read("week-7-again.png", Map.of(A_DAY, Hours.of(7))));

    // Then
    assertThat(timesheet.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(6));
    assertThat(timesheet.daysInDisputeBetweenClientTimesheets()).containsExactly(A_DAY);
  }

  @Test
  void booksTheHoursWorkedThatOverlappingClientsTimesheetsAgreeOn() {
    // When
    importAll(read("week-7.png", Map.of(A_DAY, Hours.of(8))), read("week-7-again.png", Map.of(A_DAY, Hours.of(8))));

    // Then
    assertThat(timesheet.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(8));
    assertThat(timesheet.daysInDisputeBetweenClientTimesheets()).isEmpty();
  }

  @Test
  void booksNothingOutsideTheMonth() {
    // When
    importAll(read("week-9.png", Map.of(LocalDate.of(2028, 3, 1), Hours.of(8))));

    // Then
    assertThat(timesheet.totalHours(WORKED)).isEqualTo(Hours.ZERO);
  }

  @Test
  void booksNoHoursWorkedThatWouldMakeADayExceed24Hours() {
    // Given
    timesheet.bookHours(A_DAY, STAND_BY, Hours.of(20));

    // When
    importAll(read("week-7.png", Map.of(A_DAY, Hours.of(8))));

    // Then
    assertThat(timesheet.day(A_DAY).hours(WORKED)).isEqualTo(Hours.ZERO);
  }

  @Test
  void listsTheDaysBookedAsTheClientsTimesheetsReportThem() {
    // Given
    importAll(read("week-6.png", Map.of(LocalDate.of(2028, 2, 7), Hours.of(8), LocalDate.of(2028, 2, 8), Hours.of(8))));

    // When
    timesheet.bookHours(LocalDate.of(2028, 2, 8), WORKED, Hours.of(6));

    // Then
    assertThat(timesheet.daysBookedAsReportedByClient()).containsExactly(LocalDate.of(2028, 2, 7));
  }

  @Test
  void listsTheWeekdaysTheClientsTimesheetsDoNotCover() {
    // Given
    var may = new TimesheetPrefill(new DutchPublicHolidays())
        .prefill(YearMonth.of(2026, 5), PROFESSIONAL, Optional.of(Assignment.at(new Client("ACME"))));
    var covered = new HashMap<LocalDate, Hours>();
    LocalDate.of(2026, 5, 1).datesUntil(LocalDate.of(2026, 5, 25)).forEach(date -> covered.put(date, Hours.of(8)));

    // When
    may.importClientTimesheets(ClientTimesheets.of(List.of(read("may.pdf", covered))));

    // Then: Whit Monday 25 May is a public holiday
    assertThat(may.weekdaysNotCoveredByClientTimesheets()).containsExactly(
        LocalDate.of(2026, 5, 26), LocalDate.of(2026, 5, 27), LocalDate.of(2026, 5, 28), LocalDate.of(2026, 5, 29));
  }

  @Test
  void listsTheWeekdaysOfAnUnreadableClientsTimesheetAsNotCovered() {
    // When
    importAll(ClientTimesheet.of(clientTimesheet("scribbles.png")).unreadable());

    // Then
    assertThat(timesheet.weekdaysNotCoveredByClientTimesheets()).hasSize(21);
  }

  @Test
  void listsNoWeekdaysAsNotCoveredBeforeAnyClientsTimesheetIsImported() {
    assertThat(timesheet.weekdaysNotCoveredByClientTimesheets()).isEmpty();
  }

  @Test
  void booksTheHoursWorkedOfTheClientsTimesheetsAgainWhenRestarted() {
    // Given
    importAll(read("week-7.png", Map.of(A_DAY, Hours.of(7))));
    timesheet.bookHours(A_DAY, WORKED, Hours.of(2));

    // When
    timesheet.restartFrom(Timesheet.start(FEBRUARY_2028, PROFESSIONAL, Optional.empty()));

    // Then
    assertThat(timesheet.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(7));
  }

  private void importAll(ClientTimesheet... imported) {
    timesheet.importClientTimesheets(ClientTimesheets.of(List.of(imported)));
  }

  private static ClientTimesheet read(String fileName, Map<LocalDate, Hours> hoursWorked) {
    return ClientTimesheet.of(clientTimesheet(fileName)).readAs(new ClientReportedHours(hoursWorked));
  }

  private static ClientTimesheetDocument clientTimesheet(String fileName) {
    return new ClientTimesheetDocument(fileName, DocumentFormat.PDF, fileName.getBytes());
  }

  @Test
  void canBeRestoredAsItWasKept() {
    // Given
    timesheet.bookHours(A_DAY, WORKED, Hours.of(8));
    var clientTimesheets = ClientTimesheets.of(List.of(read("acme.pdf", Map.of(A_DAY, Hours.of(7)))));

    // When
    var restored = restore(timesheet.days(), Optional.of(SUBMISSION), clientTimesheets);

    // Then
    assertThat(restored.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(8));
    assertThat(restored.submittedOn()).contains(SUBMISSION);
    assertThat(restored.previouslySubmittedOn()).contains(FIRST_SUBMISSION);
    assertThat(restored.clientTimesheets()).isEqualTo(clientTimesheets);
  }

  @Test
  void cannotBeRestoredWithoutEveryDayOfTheMonth() {
    assertThatThrownBy(() -> restore(timesheet.days().subList(0, 28), Optional.empty(), ClientTimesheets.NONE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Timesheet restore(List<DayEntry> days, Optional<LocalDate> submittedOn,
      ClientTimesheets clientTimesheets) {
    return Timesheet.restore(FEBRUARY_2028, PROFESSIONAL, Optional.of(Assignment.at(new Client("ACME"))), days,
        submittedOn, Optional.of(FIRST_SUBMISSION), clientTimesheets);
  }

  @Test
  void listsTheWeeksThatDoNotAddUpToAStandardWorkWeek() {
    // Given
    book(MONDAY_7, 8, 8, 8, 7, 8);

    // When
    var deviating = timesheet.weeksDeviatingFromStandardWorkWeek();

    // Then
    assertThat(deviating).filteredOn(week -> week.firstDay().equals(MONDAY_7))
        .singleElement()
        .satisfies(week -> assertThat(week.bookedHours()).isEqualTo(Hours.of(39)));
  }

  @Test
  void acceptsAWeekWhoseHoursAreSpreadUnevenlyOverItsDays() {
    // Given
    book(MONDAY_7, 10, 6, 8, 8, 8);

    // When / Then
    assertThat(timesheet.weeksDeviatingFromStandardWorkWeek()).extracting(WorkWeek::firstDay)
        .doesNotContain(MONDAY_7);
  }

  @Test
  void suggestsLeaveForTheHoursNotWorkedOnDaysTheClientsTimesheetsReport() {
    // When
    importWeek6(8, 8, 0, 8, 8);

    // Then
    assertThat(timesheet.day(MONDAY_7.plusDays(2)).hours(LEAVE)).isEqualTo(Hours.of(8));
    assertThat(timesheet.daysWithSuggestedLeave()).containsExactly(MONDAY_7.plusDays(2));
  }

  @Test
  void suggestsNoLeaveWhenTheWeekIsFull() {
    // When
    importWeek6(10, 6, 8, 8, 8);

    // Then
    assertThat(timesheet.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void suggestsNoMoreLeaveThanTheWeekIsShort() {
    // When
    importWeek6(10, 4, 8, 8, 8);

    // Then
    assertThat(timesheet.day(MONDAY_7.plusDays(1)).hours(LEAVE)).isEqualTo(Hours.of(2));
  }

  @Test
  void suggestsNoLeaveOnDaysTheClientsTimesheetsDoNotReport() {
    // When
    importAll(read("week-6.png", Map.of(MONDAY_7, Hours.of(8), MONDAY_7.plusDays(1), Hours.of(8))));

    // Then
    assertThat(timesheet.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void suggestsNoLeaveOnDaysTheClientsTimesheetsDisagreeAbout() {
    // When
    importAll(read("week-6.png", Map.of(MONDAY_7, Hours.of(0))), read("week-6b.png", Map.of(MONDAY_7, Hours.of(4))));

    // Then
    assertThat(timesheet.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void suggestsNoLeaveOnPublicHolidays() {
    // Given: Ascension Day on Thursday 14 May 2026
    var may = new TimesheetPrefill(new DutchPublicHolidays())
        .prefill(YearMonth.of(2026, 5), PROFESSIONAL, Optional.of(Assignment.at(new Client("ACME"))));
    var reported = new HashMap<LocalDate, Hours>();
    LocalDate.of(2026, 5, 11).datesUntil(LocalDate.of(2026, 5, 16)).forEach(date -> reported.put(date, Hours.of(8)));
    reported.put(LocalDate.of(2026, 5, 14), Hours.ZERO);

    // When
    may.importClientTimesheets(ClientTimesheets.of(List.of(read("week-20.png", reported))));

    // Then
    assertThat(may.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void suggestsNoLeaveOnDaysWithLeaveBookedByTheProfessional() {
    // Given
    timesheet.rebookDay(MONDAY_7.plusDays(2), Map.of(LEAVE, Hours.of(4)));

    // When
    importWeek6(8, 8, 0, 8, 8);

    // Then
    assertThat(timesheet.day(MONDAY_7.plusDays(2)).hours(LEAVE)).isEqualTo(Hours.of(4));
    assertThat(timesheet.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void confirmsTheSuggestedLeaveOnceTheDayIsChanged() {
    // Given
    importWeek6(8, 8, 0, 8, 8);

    // When
    timesheet.rebookDay(MONDAY_7.plusDays(2), Map.of(LEAVE, Hours.of(8)));

    // Then
    assertThat(timesheet.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void replacesUnconfirmedSuggestedLeaveWhenImportingAgain() {
    // Given
    importWeek6(8, 8, 0, 8, 8);

    // When
    importWeek6(8, 8, 8, 8, 8);

    // Then
    assertThat(timesheet.day(MONDAY_7.plusDays(2)).hours(LEAVE)).isEqualTo(Hours.ZERO);
    assertThat(timesheet.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void keepsConfirmedLeaveWhenImportingAgain() {
    // Given
    importWeek6(8, 8, 0, 8, 8);
    timesheet.rebookDay(MONDAY_7.plusDays(2), Map.of(LEAVE, Hours.of(8)));

    // When
    importWeek6(8, 8, 0, 8, 8);

    // Then
    assertThat(timesheet.day(MONDAY_7.plusDays(2)).hours(LEAVE)).isEqualTo(Hours.of(8));
    assertThat(timesheet.daysWithSuggestedLeave()).isEmpty();
  }

  @Test
  void suggestsLeaveAgainWhenRestarted() {
    // Given
    importWeek6(8, 8, 0, 8, 8);
    timesheet.rebookDay(MONDAY_7.plusDays(2), Map.of(OTHER, Hours.of(8)));

    // When
    timesheet.restartFrom(Timesheet.start(FEBRUARY_2028, PROFESSIONAL, Optional.empty()));

    // Then
    assertThat(timesheet.daysWithSuggestedLeave()).containsExactly(MONDAY_7.plusDays(2));
  }

  /**
   * Books hours worked from Monday to Friday of the week starting on the given Monday.
   */
  private void book(LocalDate monday, int... hoursWorked) {
    for (var i = 0; i < hoursWorked.length; i++) {
      timesheet.bookHours(monday.plusDays(i), WORKED, Hours.of(hoursWorked[i]));
    }
  }

  /**
   * Imports a client's timesheet reporting hours worked from Monday 7 to Friday 11 February 2028.
   */
  private void importWeek6(int... hoursWorked) {
    var reported = new HashMap<LocalDate, Hours>();
    for (var i = 0; i < hoursWorked.length; i++) {
      reported.put(MONDAY_7.plusDays(i), Hours.of(hoursWorked[i]));
    }
    importAll(read("week-6.png", reported));
  }

}
