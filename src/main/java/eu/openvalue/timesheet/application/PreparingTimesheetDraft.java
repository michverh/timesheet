package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.Timesheet;
import java.util.Objects;

/**
 * Gives the draft of a month, prefilled until something in it changes, and prefills it again when reset.
 */
public final class PreparingTimesheetDraft implements PrepareTimesheetDraftUseCase, ResetTimesheetDraftUseCase {

  private final CurrentTimesheets currentTimesheets;
  private final Timesheets timesheets;

  public PreparingTimesheetDraft(CurrentTimesheets currentTimesheets, Timesheets timesheets) {
    this.currentTimesheets = Objects.requireNonNull(currentTimesheets, "currentTimesheets");
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
  }

  @Override
  public Timesheet prepareDraft(PrepareTimesheetDraftCommand command) {
    return currentTimesheets.of(command.professional(), command.month());
  }

  /**
   * Keeps nothing for a month nothing was kept for yet: there is nothing to discard.
   */
  @Override
  public Timesheet resetDraft(ResetTimesheetDraftCommand command) {
    var prepared = currentTimesheets.prefilled(command.professional(), command.month());
    return ConcurrentChanges.retried(() -> restartFrom(prepared));
  }

  private Timesheet restartFrom(Timesheet prepared) {
    var existing = timesheets.find(prepared.professional(), prepared.month());
    if (existing.isEmpty()) {
      return prepared;
    }
    var draft = existing.get();
    draft.restartFrom(prepared);
    timesheets.save(draft);
    return draft;
  }
}
