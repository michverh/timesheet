package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Timesheet;

public interface PrepareTimesheetDraftUseCase {

  /**
   * Gives the draft timesheet of a professional for a month. When there is none yet, it is prepared first: prefilled
   * from the standard work week, the public holidays, the registered absences and the professional's assignment.
   * When the registered absences cannot be retrieved, it is prefilled as if there were none, and marked as such.
   */
  Timesheet prepareDraft(PrepareTimesheetDraftCommand command);
}
