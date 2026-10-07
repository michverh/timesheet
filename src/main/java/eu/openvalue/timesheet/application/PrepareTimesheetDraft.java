package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Prepares prefilled drafts: once when a draft is first needed, and again when the professional resets it.
 */
public final class PrepareTimesheetDraft implements PrepareTimesheetDraftUseCase, ResetTimesheetDraftUseCase {

  private final TimesheetPrefill prefill;
  private final AbsenceRegistry absenceRegistry;
  private final Assignments assignments;
  private final TimesheetDrafts drafts;

  public PrepareTimesheetDraft(TimesheetPrefill prefill, AbsenceRegistry absenceRegistry, Assignments assignments,
      TimesheetDrafts drafts) {
    this.prefill = Objects.requireNonNull(prefill, "prefill");
    this.absenceRegistry = Objects.requireNonNull(absenceRegistry, "absenceRegistry");
    this.assignments = Objects.requireNonNull(assignments, "assignments");
    this.drafts = Objects.requireNonNull(drafts, "drafts");
  }

  @Override
  public Timesheet prepareDraft(PrepareTimesheetDraftCommand command) {
    var professional = command.professional();
    var month = command.month();
    return drafts.find(professional, month).orElseGet(() -> prepareNewDraft(professional, month));
  }

  @Override
  public Timesheet resetDraft(ResetTimesheetDraftCommand command) {
    return prepareNewDraft(command.professional(), command.month());
  }

  private Timesheet prepareNewDraft(Professional professional, YearMonth month) {
    var draft = prefill.prefill(month, professional, assignments.assignmentOf(professional, month),
        absenceRegistry.absencesOf(professional, month));
    drafts.save(draft);
    return draft;
  }
}
