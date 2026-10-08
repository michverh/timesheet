package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A professional's monthly account of hours, kilometres and explanations, with exactly one entry for every day of
 * the month.
 */
public final class Timesheet {

  private final YearMonth month;
  private final Professional professional;
  private final SortedMap<LocalDate, DayEntry> days = new TreeMap<>();
  private Assignment assignment;
  private LocalDate submittedOn;
  private LocalDate previouslySubmittedOn;
  private boolean registeredAbsencesUnknown;

  private Timesheet(YearMonth month, Professional professional, Assignment assignment) {
    this.month = month;
    this.professional = professional;
    this.assignment = assignment;
    month.atDay(1).datesUntil(month.atEndOfMonth().plusDays(1)).forEach(date -> days.put(date, DayEntry.empty(date)));
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

  public YearMonth month() {
    return month;
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
    return List.copyOf(days.values());
  }

  public DayEntry day(LocalDate date) {
    return days.get(requireInMonth(date));
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

  public Hours totalHours(HourCategory category) {
    return days.values().stream().map(day -> day.hours(category)).reduce(Hours.ZERO, Hours::plus);
  }

  public Kilometres totalKilometres() {
    return days.values().stream().map(DayEntry::kilometres).reduce(Kilometres.ZERO, Kilometres::plus);
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

  /**
   * The days with hours in a category that asks for an explanation, but without one.
   */
  public List<LocalDate> daysLackingRequiredExplanation() {
    return days.values().stream().filter(DayEntry::lacksRequiredExplanation).map(DayEntry::date).toList();
  }

  /**
   * Starts over from a freshly prepared timesheet for the same professional and month, discarding all changes. Earlier
   * submissions are remembered, so a correction remains a correction.
   *
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  public void restartFrom(Timesheet prepared) {
    Objects.requireNonNull(prepared, "prepared");
    requireNotSubmitted();
    if (!prepared.month.equals(month) || !prepared.professional.equals(professional)) {
      throw new IllegalArgumentException("Can only restart from a timesheet of %s for %s".formatted(
          professional.email(), month));
    }
    assignment = prepared.assignment;
    days.clear();
    days.putAll(prepared.days);
    registeredAbsencesUnknown = prepared.registeredAbsencesUnknown;
  }

  /**
   * Checks whether the timesheet can be submitted, without submitting it.
   *
   * @throws FutureMonthNotSubmittableException when the month has not started yet on {@code today}
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted before
   */
  public void ensureSubmittableOn(LocalDate today) {
    Objects.requireNonNull(today, "today");
    requireNotSubmitted();
    if (month.isAfter(YearMonth.from(today))) {
      throw new FutureMonthNotSubmittableException(month);
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
    submittedOn = today;
  }

  /**
   * Makes a submitted timesheet changeable again, so it can be corrected and submitted once more.
   *
   * @throws TimesheetNotSubmittedException when the timesheet has not been submitted
   */
  public void reopenForCorrection() {
    if (!isSubmitted()) {
      throw new TimesheetNotSubmittedException(month);
    }
    previouslySubmittedOn = submittedOn;
    submittedOn = null;
  }

  public boolean isSubmitted() {
    return submittedOn != null;
  }

  public Optional<LocalDate> submittedOn() {
    return Optional.ofNullable(submittedOn);
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
   * When the timesheet was last submitted before it was reopened for correction, if it ever was.
   */
  public Optional<LocalDate> previouslySubmittedOn() {
    return Optional.ofNullable(previouslySubmittedOn);
  }

  private void update(DayEntry entry) {
    requireNotSubmitted();
    days.put(entry.date(), entry);
  }

  private void requireNotSubmitted() {
    if (isSubmitted()) {
      throw new TimesheetAlreadySubmittedException(month);
    }
  }

  private LocalDate requireInMonth(LocalDate date) {
    Objects.requireNonNull(date, "date");
    if (!YearMonth.from(date).equals(month)) {
      throw new IllegalArgumentException("%s is not part of the timesheet for %s".formatted(date, month));
    }
    return date;
  }
}
