package eu.openvalue.timesheet.application.port.in;

public interface SubmitTimesheetUseCase {

  /**
   * Hands the draft timesheet of a professional for a month in to HR, after which it can no longer be changed until
   * it is reopened for correction.
   *
   * @throws TimesheetSubmissionFailedException when the timesheet could not be handed in; it then remains a draft
   */
  void submit(SubmitTimesheetCommand command);
}
