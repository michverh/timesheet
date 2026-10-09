package eu.openvalue.timesheet.domain;

import eu.openvalue.timesheet.domain.SubmissionState.Draft;
import eu.openvalue.timesheet.domain.SubmissionState.Submitted;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A professional's monthly account of hours, kilometres and explanations, with exactly one entry for every day of
 * the month().
 */
public final class Timesheet {

  private final Professional professional;
  private Assignment assignment;
  private MonthOfDays days;
  private SubmissionState submission = Draft.NEW;
  private boolean registeredAbsencesUnknown;
  private ClientTimesheet clientTimesheet;

  private Timesheet(YearMonth month, Professional professional, Assignment assignment) {
    this.professional = professional;
    this.assignment = assignment;
    this.days = MonthOfDays.emptyFor(month);
  }

  /**
   * Starts an empty timesheet for the given month().
   *
   * @param assignment the client the professional works for, or empty when the professional is on the bench
   */
  public static Timesheet start(YearMonth month, Professional professional, Optional<Assignment> assignment) {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(assignment, "assignment");
    return new Timesheet(month, professional, assignment.orElse(null));
  }

  /**
   * Restores a timesheet as it was kept, for instance by a repository, refusing states it could never have reached.
   *
   * @param days exactly one entry for every day of the month
   * @param submittedOn when the timesheet was submitted, or empty while it is a draft
   * @param previouslySubmittedOn when it was submitted before being reopened for correction, if it ever was
   * @param clientTimesheet the client's own timesheet handed in alongside, if any
   * @throws IllegalArgumentException when the state is not one a timesheet can be in
   */
  public static Timesheet restore(YearMonth month, Professional professional, Optional<Assignment> assignment,
      List<DayEntry> days, boolean registeredAbsencesUnknown, Optional<LocalDate> submittedOn,
      Optional<LocalDate> previouslySubmittedOn, Optional<ClientTimesheet> clientTimesheet) {
    Objects.requireNonNull(days, "days");
    Objects.requireNonNull(clientTimesheet, "clientTimesheet");
    var timesheet = start(month, professional, assignment);
    timesheet.days = MonthOfDays.of(month, days);
    timesheet.registeredAbsencesUnknown = registeredAbsencesUnknown;
    timesheet.submission = SubmissionState.of(submittedOn, previouslySubmittedOn);
    timesheet.clientTimesheet = clientTimesheet.orElse(null);
    return timesheet;
  }

  public YearMonth month() {
    return days.month();
  }

  public Professional professional() {
    return professional;
  }

  public Optional<Assignment> assignment() {
    return Optional.ofNullable(assignment);
  }

  public boolean isOnBench() {
    return assignment == null;
  }

  public List<DayEntry> days() {
    return days.all();
  }

  /**
   * @throws IllegalArgumentException when the date is not part of the month
   */
  public DayEntry day(LocalDate date) {
    return days.day(date);
  }

  /**
   * Sets the hours booked in a category on a day, replacing what was booked in that category before. Booking zero
   * hours clears the category.
   *
   * @throws DailyHoursLimitExceededException when the day's total would exceed 24 hours
   */
  public void bookHours(LocalDate date, HourCategory category, Hours hours) {
    update(day(date).withHours(category, hours));
  }

  /**
   * Replaces all hours booked on a day at once. Categories that are absent or zero are cleared.
   *
   * @throws DailyHoursLimitExceededException when the day's total would exceed 24 hours
   */
  public void rebookDay(LocalDate date, Map<HourCategory, Hours> hours) {
    update(day(date).withBookedHours(Objects.requireNonNull(hours, "hours")));
  }

  public void recordKilometres(LocalDate date, Kilometres kilometres) {
    update(day(date).withKilometres(kilometres));
  }

  public void explain(LocalDate date, Explanation explanation) {
    update(day(date).withExplanation(Objects.requireNonNull(explanation, "explanation")));
  }

  public void removeExplanation(LocalDate date) {
    update(day(date).withExplanation(null));
  }

  public Hours totalHours(HourCategory category) {
    return days.totalHours(category);
  }

  public Kilometres totalKilometres() {
    return days.totalKilometres();
  }

  /**
   * The weekdays on which the hours filling the workday add up to more or less than the standard workday.
   */
  public List<DayEntry> daysDeviatingFromStandardWorkday() {
    return days.daysDeviatingFromStandardWorkday();
  }

  /**
   * The days with hours in a category that asks for an explanation, but without one.
   */
  public List<LocalDate> daysLackingRequiredExplanation() {
    return days.daysLackingRequiredExplanation();
  }

  /**
   * Hands in the client's own timesheet alongside this one, replacing any handed in before.
   *
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void attachClientTimesheet(ClientTimesheetDocument document) {
    Objects.requireNonNull(document, "document");
    requireDraft();
    clientTimesheet = ClientTimesheet.handedIn(document);
  }

  /**
   * Records the hours worked as read from the client's timesheet, so they can be compared with the hours booked.
   *
   * @throws IllegalStateException when no client's timesheet has been handed in
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void recordHoursReportedByClient(ClientReportedHours reported) {
    clientTimesheet = requireClientTimesheet().readAs(reported);
  }

  /**
   * Records that the hours worked could not be read from the client's timesheet, so they need to be compared by hand.
   *
   * @throws IllegalStateException when no client's timesheet has been handed in
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void markClientTimesheetUnreadable() {
    clientTimesheet = requireClientTimesheet().unreadable();
  }

  /**
   * Whether the client's timesheet was read, but concerns another period: it covers none of the days of the month().
   */
  public boolean isClientTimesheetOfAnotherPeriod() {
    return clientTimesheet().filter(client -> client.isOfAnotherPeriodThan(month())).isPresent();
  }

  /**
   * The days on which the hours booked as worked differ from the client's timesheet; none when it has not been read.
   */
  public List<HoursDifference> differencesWithClientTimesheet() {
    return clientTimesheet().map(client -> client.differencesWith(days)).orElse(List.of());
  }

  /**
   * The differences with the client's timesheet that taking over its hours would resolve: those on days with nothing
   * else filling the workday. Days with, for instance, leave or sickness are left to the professional.
   */
  public List<HoursDifference> differencesToTakeOverFromClient() {
    return differencesWithClientTimesheet().stream()
        .filter(difference -> day(difference.date()).canTakeOverHoursWorked(difference.reportedByClient()))
        .toList();
  }

  /**
   * Books the hours worked according to the client's timesheet on the days where they differ and nothing else fills
   * the workday. Hours no longer worked are not booked elsewhere.
   *
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void takeOverHoursReportedByClient() {
    requireDraft();
    differencesToTakeOverFromClient()
        .forEach(difference -> bookHours(difference.date(), HourCategory.WORKED, difference.reportedByClient()));
  }

  public Optional<ClientTimesheet> clientTimesheet() {
    return Optional.ofNullable(clientTimesheet);
  }

  /**
   * Whether the professional works for a client, but the client's own timesheet has not been handed in.
   */
  public boolean lacksClientTimesheet() {
    return !isOnBench() && clientTimesheet == null;
  }

  /**
   * Starts over from a freshly prepared timesheet for the same professional and month(), discarding all changes to the
   * days. Earlier submissions and the client's timesheet are kept, so a correction remains a correction.
   *
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void restartFrom(Timesheet prepared) {
    Objects.requireNonNull(prepared, "prepared");
    requireDraft();
    if (!prepared.month().equals(month()) || !prepared.professional.equals(professional)) {
      throw new IllegalArgumentException("Can only restart from a timesheet of %s for %s".formatted(
          professional.email(), month()));
    }
    assignment = prepared.assignment;
    days = prepared.days;
    registeredAbsencesUnknown = prepared.registeredAbsencesUnknown;
  }

  /**
   * Whether the timesheet was prefilled without knowing the absences HR registered, so leave and sickness may be
   * missing.
   */
  public boolean registeredAbsencesUnknown() {
    return registeredAbsencesUnknown;
  }

  void markRegisteredAbsencesUnknown() {
    registeredAbsencesUnknown = true;
  }

  /**
   * Checks whether the timesheet can be submitted, without submitting it.
   *
   * @throws FutureMonthNotSubmittableException when the month() has not started yet on {@code today}
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted before
   */
  public void ensureSubmittableOn(LocalDate today) {
    Objects.requireNonNull(today, "today");
    requireDraft();
    if (month().isAfter(YearMonth.from(today))) {
      throw new FutureMonthNotSubmittableException(month());
    }
  }

  /**
   * Submits the timesheet, after which it can no longer be changed until it is reopened for correction.
   *
   * @throws FutureMonthNotSubmittableException when the month() has not started yet on {@code today}
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted before
   */
  public void submit(LocalDate today) {
    ensureSubmittableOn(today);
    submission = requireDraft().submit(today);
  }

  /**
   * Makes a submitted timesheet changeable again, so it can be corrected and submitted once more.
   *
   * @throws TimesheetNotSubmittedException when the timesheet has not been submitted
   */
  public void reopenForCorrection() {
    if (!(submission instanceof Submitted submitted)) {
      throw new TimesheetNotSubmittedException(month());
    }
    submission = submitted.reopen();
  }

  public boolean isSubmitted() {
    return submission instanceof Submitted;
  }

  public Optional<LocalDate> submittedOn() {
    return submission instanceof Submitted submitted ? Optional.of(submitted.on()) : Optional.empty();
  }

  /**
   * When the timesheet was last submitted before it was reopened for correction, if it ever was.
   */
  public Optional<LocalDate> previouslySubmittedOn() {
    return submission.previouslySubmittedOn();
  }

  private void update(DayEntry entry) {
    requireDraft();
    days = days.with(entry);
  }

  private ClientTimesheet requireClientTimesheet() {
    requireDraft();
    if (clientTimesheet == null) {
      throw new IllegalStateException("No client's timesheet has been handed in for " + month());
    }
    return clientTimesheet;
  }

  private Draft requireDraft() {
    if (submission instanceof Draft draft) {
      return draft;
    }
    throw new TimesheetAlreadySubmittedException(month());
  }
}
