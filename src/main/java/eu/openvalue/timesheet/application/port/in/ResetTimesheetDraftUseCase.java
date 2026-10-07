package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Timesheet;

public interface ResetTimesheetDraftUseCase {

  /**
   * Discards all changes to the draft timesheet of a professional for a month, and prefills it again from the
   * standard work week, the public holidays, the registered absences and the professional's assignment.
   *
   * @throws eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  Timesheet resetDraft(ResetTimesheetDraftCommand command);
}
