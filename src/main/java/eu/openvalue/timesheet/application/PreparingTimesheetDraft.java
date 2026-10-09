package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistryUnavailableException;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Prepares prefilled drafts: once when a draft is first needed, and again when the professional resets it.
 */
public final class PreparingTimesheetDraft implements PrepareTimesheetDraftUseCase, ResetTimesheetDraftUseCase {

  private static final Logger LOGGER = System.getLogger(PreparingTimesheetDraft.class.getName());

  private final TimesheetPrefill prefill;
  private final AbsenceRegistry absenceRegistry;
  private final Assignments assignments;
  private final Timesheets timesheets;

  public PreparingTimesheetDraft(TimesheetPrefill prefill, AbsenceRegistry absenceRegistry, Assignments assignments,
      Timesheets timesheets) {
    this.prefill = Objects.requireNonNull(prefill, "prefill");
    this.absenceRegistry = Objects.requireNonNull(absenceRegistry, "absenceRegistry");
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

  /**
   * Prefills the month from the registered absences, or as if there were none when the registry cannot be reached,
   * so the professional can still fill in their timesheet.
   */
  private Timesheet prefilled(Professional professional, YearMonth month) {
    var assignment = assignments.assignmentOf(professional, month);
    try {
      return prefill.prefill(month, professional, assignment, absenceRegistry.absencesOf(professional, month));
    } catch (AbsenceRegistryUnavailableException e) {
      LOGGER.log(Level.WARNING, "Prefilling the timesheet of %s for %s without registered absences"
          .formatted(professional.email(), month), e);
      return prefill.prefillWithoutRegisteredAbsences(month, professional, assignment);
    }
  }
}
