package eu.openvalue.timesheet.domain;

import eu.openvalue.timesheet.domain.SubmissionState.Draft;
import eu.openvalue.timesheet.domain.SubmissionState.Submitted;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A professional's monthly account of hours, kilometres and explanations, with exactly one entry for every day of
 * the month.
 */
public final class Timesheet {

  private final Professional professional;
  private Assignment assignment;
  private MonthOfDays days;
  private SubmissionState submission = Draft.NEW;
  private ClientTimesheets clientTimesheets = ClientTimesheets.NONE;

  private Timesheet(YearMonth month, Professional professional, Assignment assignment) {
    this.professional = professional;
    this.assignment = assignment;
    this.days = MonthOfDays.emptyFor(month);
  }

  /**
   * Starts an empty timesheet for the given month.
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
   * @param clientTimesheets the client's own timesheets imported into it
   * @throws IllegalArgumentException when the state is not one a timesheet can be in
   */
  public static Timesheet restore(YearMonth month, Professional professional, Optional<Assignment> assignment,
      List<DayEntry> days, Optional<LocalDate> submittedOn, Optional<LocalDate> previouslySubmittedOn,
      ClientTimesheets clientTimesheets) {
    Objects.requireNonNull(days, "days");
    Objects.requireNonNull(clientTimesheets, "clientTimesheets");
    var timesheet = start(month, professional, assignment);
    timesheet.days = MonthOfDays.of(month, days);
    timesheet.submission = SubmissionState.of(submittedOn, previouslySubmittedOn);
    timesheet.clientTimesheets = clientTimesheets;
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
   * Replaces all hours booked on a day at once. Categories that are absent or zero are cleared. This confirms any leave
   * filled in on the day.
   *
   * @throws DailyHoursLimitExceededException when the day's total would exceed 24 hours
   */
  public void rebookDay(LocalDate date, Map<HourCategory, Hours> hours) {
    update(day(date).withBookedHours(Objects.requireNonNull(hours, "hours")).confirmed());
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
   * The weeks in which the hours filling the workday add up to more or less than a standard workday for each of its
   * weekdays, however they are spread over the days.
   */
  public List<WorkWeek> weeksDeviatingFromStandardWorkWeek() {
    return days.weeks().stream().filter(WorkWeek::deviatesFromStandardWorkWeek).toList();
  }

  /**
   * The days with leave filled in for hours that were expected, but not worked, which the professional has yet to
   * confirm.
   */
  public List<LocalDate> daysWithSuggestedLeave() {
    return days().stream().filter(DayEntry::isLeaveSuggested).map(DayEntry::date).toList();
  }

  /**
   * The days with hours in a category that asks for an explanation, but without one.
   */
  public List<LocalDate> daysLackingRequiredExplanation() {
    return days.daysLackingRequiredExplanation();
  }

  /**
   * Imports the client's own timesheets for the month, as read, replacing those imported before, and books the
   * hours worked that they agree on. Days they disagree about, or that they do not cover, are left as they are.
   *
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void importClientTimesheets(ClientTimesheets imported) {
    Objects.requireNonNull(imported, "imported");
    requireDraft();
    clientTimesheets = imported;
    bookAsReportedByClient();
  }

  public ClientTimesheets clientTimesheets() {
    return clientTimesheets;
  }

  /**
   * Whether the professional works for a client, but no timesheet of the client has been imported.
   */
  public boolean lacksClientTimesheet() {
    return !isOnBench() && clientTimesheets.isEmpty();
  }

  /**
   * The days on which the client's timesheets report different hours worked, so none were booked.
   */
  public List<LocalDate> daysInDisputeBetweenClientTimesheets() {
    return List.copyOf(clientTimesheets.daysInDisputeIn(month()));
  }

  /**
   * The weekdays that are no public holiday, but are covered by none of the client's timesheets imported, so the
   * hours worked on them are not known from the client; none when no client's timesheet has been imported.
   */
  public List<LocalDate> weekdaysNotCoveredByClientTimesheets() {
    if (clientTimesheets.isEmpty()) {
      return List.of();
    }
    return days().stream()
        .filter(day -> !day.isWeekend() && day.hours(HourCategory.PUBLIC_HOLIDAY).isZero())
        .map(DayEntry::date)
        .filter(date -> !clientTimesheets.cover(date))
        .toList();
  }

  /**
   * The days on which the hours worked are booked as the client's timesheets report them.
   */
  public List<LocalDate> daysBookedAsReportedByClient() {
    return clientTimesheets.hoursWorkedIn(month()).entrySet().stream()
        .filter(reported -> day(reported.getKey()).hours(HourCategory.WORKED).equals(reported.getValue()))
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * Starts over from a freshly prepared timesheet for the same professional and month, discarding all changes to the
   * days. Earlier submissions and the client's timesheets are kept, so a correction remains a correction, and the
   * hours worked they agree on are booked again.
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
    bookAsReportedByClient();
  }

  /**
   * Checks whether the timesheet can still be changed, without changing it.
   *
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void ensureChangeable() {
    requireDraft();
  }

  /**
   * Checks whether the timesheet can be submitted, without submitting it.
   *
   * @throws FutureMonthNotSubmittableException when the month has not started yet on {@code today}
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
   * @throws FutureMonthNotSubmittableException when the month has not started yet on {@code today}
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

  /**
   * Books the hours worked that the client's timesheets agree on, and fills in leave for the hours that were expected,
   * but not worked, replacing leave filled in before that was not confirmed.
   */
  private void bookAsReportedByClient() {
    days.all().forEach(day -> days = days.with(day.withoutSuggestedLeave()));
    var reported = clientTimesheets.hoursWorkedIn(month());
    reported.forEach((date, hours) -> {
      var day = day(date);
      if (day.canBook(HourCategory.WORKED, hours)) {
        days = days.with(day.withHours(HourCategory.WORKED, hours));
      }
    });
    days.weeks().forEach(week -> suggestLeaveForHoursNotWorked(week, reported.keySet()));
  }

  /**
   * Fills in leave on the weekdays of the week that are short of a standard workday, as far as the week is short of a
   * standard work week. Only on days the client's timesheets report on, which are no public holiday and have no
   * leave yet: on other days, the reason for the missing hours is not known.
   */
  private void suggestLeaveForHoursNotWorked(WorkWeek week, Set<LocalDate> reported) {
    var remaining = week.shortfall();
    for (var day : week.days()) {
      var leave = Hours.min(day.shortOfStandardWorkday(), remaining);
      var suggestible = reported.contains(day.date()) && day.hours(HourCategory.PUBLIC_HOLIDAY).isZero()
          && day.hours(HourCategory.LEAVE).isZero() && day.canBook(HourCategory.LEAVE, leave);
      if (suggestible && !leave.isZero()) {
        days = days.with(day.withSuggestedLeave(leave));
        remaining = remaining.minus(leave);
      }
    }
  }

  private void update(DayEntry entry) {
    requireDraft();
    days = days.with(entry);
  }

  private Draft requireDraft() {
    if (submission instanceof Draft draft) {
      return draft;
    }
    throw new TimesheetAlreadySubmittedException(month());
  }
}
