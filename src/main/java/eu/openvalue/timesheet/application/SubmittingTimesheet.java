package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.SubmitTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.in.TimesheetSubmissionFailedException;
import eu.openvalue.timesheet.application.port.out.TimesheetDelivery;
import eu.openvalue.timesheet.application.port.out.TimesheetDeliveryFailedException;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.application.port.out.TimesheetSubmission;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Submits a timesheet only once it has been handed in, so a failed delivery leaves it a draft that can be submitted
 * again.
 */
public final class SubmittingTimesheet implements SubmitTimesheetUseCase {

  private final TimesheetDrafts drafts;
  private final TimesheetSpreadsheet spreadsheet;
  private final TimesheetDelivery delivery;
  private final Clock clock;

  public SubmittingTimesheet(TimesheetDrafts drafts, TimesheetSpreadsheet spreadsheet, TimesheetDelivery delivery,
      Clock clock) {
    this.drafts = Objects.requireNonNull(drafts, "drafts");
    this.spreadsheet = Objects.requireNonNull(spreadsheet, "spreadsheet");
    this.delivery = Objects.requireNonNull(delivery, "delivery");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public void submit(SubmitTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = drafts.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    var today = LocalDate.now(clock);
    draft.ensureSubmittableOn(today);

    var file = spreadsheet.create(draft);
    try {
      delivery.deliver(new TimesheetSubmission(professional, month, today, draft.previouslySubmittedOn(),
          SpreadsheetFileName.of(draft, file.fileExtension()), file));
    } catch (TimesheetDeliveryFailedException e) {
      throw new TimesheetSubmissionFailedException(month, e);
    }

    draft.submit(today);
    drafts.save(draft);
  }
}
