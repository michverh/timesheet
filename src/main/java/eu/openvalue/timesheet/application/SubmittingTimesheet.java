package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.SubmitTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.in.TimesheetSubmissionFailedException;
import eu.openvalue.timesheet.application.port.out.TimesheetChangedConcurrentlyException;
import eu.openvalue.timesheet.application.port.out.TimesheetDelivery;
import eu.openvalue.timesheet.application.port.out.TimesheetDeliveryFailedException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.application.port.out.TimesheetSubmission;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Submits a timesheet only once it has been handed in, so a failed delivery leaves it a draft that can be submitted
 * again.
 */
public final class SubmittingTimesheet implements SubmitTimesheetUseCase {

  private final Timesheets timesheets;
  private final TimesheetSpreadsheet spreadsheet;
  private final TimesheetDelivery delivery;
  private final Clock clock;

  public SubmittingTimesheet(Timesheets timesheets, TimesheetSpreadsheet spreadsheet, TimesheetDelivery delivery,
      Clock clock) {
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
    this.spreadsheet = Objects.requireNonNull(spreadsheet, "spreadsheet");
    this.delivery = Objects.requireNonNull(delivery, "delivery");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public void submit(SubmitTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = timesheets.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    var today = LocalDate.now(clock);
    draft.ensureSubmittableOn(today);

    var file = spreadsheet.create(draft);
    try {
      delivery.deliver(new TimesheetSubmission(professional, month, today, draft.previouslySubmittedOn(),
          SpreadsheetFileName.of(draft, file.fileExtension()), file, draft.clientTimesheets().documents()));
    } catch (TimesheetDeliveryFailedException e) {
      throw new TimesheetSubmissionFailedException(month, e);
    }

    markSubmitted(draft, today);
  }

  /**
   * Marks the timesheet submitted once it has been delivered. When it was changed in the meantime, the timesheet as it
   * is kept then is marked submitted, rather than delivering it once more.
   */
  private void markSubmitted(Timesheet delivered, LocalDate today) {
    try {
      delivered.submit(today);
      timesheets.save(delivered);
    } catch (TimesheetChangedConcurrentlyException e) {
      ConcurrentChanges.retried(() -> {
        var latest = timesheets.find(delivered.professional(), delivered.month()).orElseThrow();
        latest.submit(today);
        timesheets.save(latest);
      });
    }
  }
}
