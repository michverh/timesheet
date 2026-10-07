package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.util.Objects;

public final class PrepareTimesheetDraft implements PrepareTimesheetDraftUseCase {

  private final TimesheetPrefill prefill;
  private final AbsenceRegistry absenceRegistry;
  private final Assignments assignments;

  public PrepareTimesheetDraft(TimesheetPrefill prefill, AbsenceRegistry absenceRegistry, Assignments assignments) {
    this.prefill = Objects.requireNonNull(prefill, "prefill");
    this.absenceRegistry = Objects.requireNonNull(absenceRegistry, "absenceRegistry");
    this.assignments = Objects.requireNonNull(assignments, "assignments");
  }

  @Override
  public Timesheet prepareDraft(PrepareTimesheetDraftCommand command) {
    var professional = command.professional();
    var month = command.month();
    return prefill.prefill(month, professional, assignments.assignmentOf(professional, month),
        absenceRegistry.absencesOf(professional, month));
  }
}
