package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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
  private final Assignment assignment;
  private final SortedMap<LocalDate, DayEntry> days = new TreeMap<>();
  private LocalDate submittedOn;

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
   * Submits the timesheet, after which it can no longer be changed.
   *
   * @throws FutureMonthNotSubmittableException when the month has not started yet on {@code today}
   * @throws TimesheetAlreadySubmittedException when the timesheet has been submitted before
   */
  public void submit(LocalDate today) {
    Objects.requireNonNull(today, "today");
    requireNotSubmitted();
    if (month.isAfter(YearMonth.from(today))) {
      throw new FutureMonthNotSubmittableException(month);
    }
    submittedOn = today;
  }

  public boolean isSubmitted() {
    return submittedOn != null;
  }

  public Optional<LocalDate> submittedOn() {
    return Optional.ofNullable(submittedOn);
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
