package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayCommand;
import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Objects;

public final class AdjustingTimesheetDay implements AdjustTimesheetDayUseCase {

  private final CurrentTimesheets currentTimesheets;
  private final Timesheets timesheets;

  public AdjustingTimesheetDay(CurrentTimesheets currentTimesheets, Timesheets timesheets) {
    this.currentTimesheets = Objects.requireNonNull(currentTimesheets, "currentTimesheets");
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
  }

  @Override
  public Timesheet adjustDay(AdjustTimesheetDayCommand command) {
    return ConcurrentChanges.retried(() -> adjust(command));
  }

  private Timesheet adjust(AdjustTimesheetDayCommand command) {
    var professional = command.professional();
    var date = command.date();
    var month = YearMonth.from(date);
    var draft = currentTimesheets.of(professional, month);

    draft.rebookDay(date, command.hours());
    draft.recordKilometres(date, command.kilometres());
    command.explanation().ifPresentOrElse(
        explanation -> draft.explain(date, explanation),
        () -> draft.removeExplanation(date));
    timesheets.save(draft);
    return draft;
  }
}
