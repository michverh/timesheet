package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Gives a professional's timesheet for a month as it was kept or, when nothing was kept for the month yet, freshly
 * prefilled. A month is only kept once something in it changes, so merely looking at a month keeps nothing.
 */
public final class CurrentTimesheets {

  private final TimesheetPrefill prefill;
  private final Assignments assignments;
  private final Timesheets timesheets;

  public CurrentTimesheets(TimesheetPrefill prefill, Assignments assignments, Timesheets timesheets) {
    this.prefill = Objects.requireNonNull(prefill, "prefill");
    this.assignments = Objects.requireNonNull(assignments, "assignments");
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
  }

  Timesheet of(Professional professional, YearMonth month) {
    return timesheets.find(professional, month).orElseGet(() -> prefilled(professional, month));
  }

  /**
   * The month prefilled from the standard work week, the public holidays and the professional's assignment.
   */
  Timesheet prefilled(Professional professional, YearMonth month) {
    return prefill.prefill(month, professional, assignments.assignmentOf(professional, month));
  }
}
