package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Timesheet;

public interface PrepareTimesheetDraftUseCase {

  /**
   * Drafts the timesheet of a professional for a month, prefilled from the standard work week, the public holidays,
   * the registered absences and the professional's assignment.
   */
  Timesheet prepareDraft(PrepareTimesheetDraftCommand command);
}
