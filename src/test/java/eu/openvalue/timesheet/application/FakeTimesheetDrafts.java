package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class FakeTimesheetDrafts implements TimesheetDrafts {

  private final List<Timesheet> drafts = new ArrayList<>();

  @Override
  public Optional<Timesheet> find(Professional professional, YearMonth month) {
    return drafts.stream()
        .filter(draft -> draft.professional().equals(professional) && draft.month().equals(month))
        .findFirst();
  }

  @Override
  public void save(Timesheet draft) {
    drafts.removeIf(saved -> saved.professional().equals(draft.professional()) && saved.month().equals(draft.month()));
    drafts.add(draft);
  }
}
