package eu.openvalue.timesheet.application.port.in;

public interface CorrectTimesheetUseCase {

  /**
   * Reopens the submitted timesheet of a professional for a month, so it can be corrected and submitted again.
   *
   * @throws TimesheetDraftNotFoundException when there is no timesheet for the month
   */
  void reopenForCorrection(CorrectTimesheetCommand command);
}
