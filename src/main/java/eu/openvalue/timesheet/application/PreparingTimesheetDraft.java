package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Prepares prefilled drafts: once when a draft is first needed, and again when the professional resets it.
 */
public final class PreparingTimesheetDraft implements PrepareTimesheetDraftUseCase, ResetTimesheetDraftUseCase {

  private final TimesheetPrefill prefill;
  private final Assignments assignments;
  private final Timesheets timesheets;

  public PreparingTimesheetDraft(TimesheetPrefill prefill, Assignments assignments, Timesheets timesheets) {
    this.prefill = Objects.requireNonNull(prefill, "prefill");
    this.assignments = Objects.requireNonNull(assignments, "assignments");
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
  }

  @Override
  public Timesheet prepareDraft(PrepareTimesheetDraftCommand command) {
    var professional = command.professional();
    var month = command.month();
    return ConcurrentChanges.retried(() -> timesheets.find(professional, month)
        .orElseGet(() -> prepareNewDraft(professional, month)));
  }

  @Override
  public Timesheet resetDraft(ResetTimesheetDraftCommand command) {
    var professional = command.professional();
    var month = command.month();
    var prepared = prefilled(professional, month);
    return ConcurrentChanges.retried(() -> restartFrom(prepared));
  }

  private Timesheet restartFrom(Timesheet prepared) {
    var existing = timesheets.find(prepared.professional(), prepared.month());
    if (existing.isEmpty()) {
      timesheets.save(prepared);
      return prepared;
    }
    var draft = existing.get();
    draft.restartFrom(prepared);
    timesheets.save(draft);
    return draft;
  }

  private Timesheet prepareNewDraft(Professional professional, YearMonth month) {
    var draft = prefilled(professional, month);
    timesheets.save(draft);
    return draft;
  }

  private Timesheet prefilled(Professional professional, YearMonth month) {
    return prefill.prefill(month, professional, assignments.assignmentOf(professional, month));
  }
}
