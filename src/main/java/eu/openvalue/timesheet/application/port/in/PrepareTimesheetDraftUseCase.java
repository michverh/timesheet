package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Timesheet;

public interface PrepareTimesheetDraftUseCase {

  /**
   * Gives the draft timesheet of a professional for a month. When there is none yet, it is prepared first: prefilled
   * from the standard work week, the public holidays and the professional's assignment.
   */
  Timesheet prepareDraft(PrepareTimesheetDraftCommand command);
}
