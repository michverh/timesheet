package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayCommand;
import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Objects;

public final class AdjustTimesheetDay implements AdjustTimesheetDayUseCase {

  private final TimesheetDrafts drafts;

  public AdjustTimesheetDay(TimesheetDrafts drafts) {
    this.drafts = Objects.requireNonNull(drafts, "drafts");
  }

  @Override
  public Timesheet adjustDay(AdjustTimesheetDayCommand command) {
    var professional = command.professional();
    var date = command.date();
    var month = YearMonth.from(date);
    var draft = drafts.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));

    draft.rebookDay(date, command.hours());
    draft.recordKilometres(date, command.kilometres());
    command.explanation().ifPresentOrElse(
        explanation -> draft.explain(date, explanation),
        () -> draft.removeExplanation(date));
    drafts.save(draft);
    return draft;
  }
}
