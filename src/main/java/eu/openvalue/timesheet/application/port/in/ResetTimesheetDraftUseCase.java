package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Timesheet;

public interface ResetTimesheetDraftUseCase {

  /**
   * Discards all changes to the draft timesheet of a professional for a month, and prefills it again from the
   * standard work week, the public holidays, the registered absences and the professional's assignment. When the
   * registered absences cannot be retrieved, it is prefilled as if there were none, and marked as such.
   *
   * @throws eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  Timesheet resetDraft(ResetTimesheetDraftCommand command);
}
