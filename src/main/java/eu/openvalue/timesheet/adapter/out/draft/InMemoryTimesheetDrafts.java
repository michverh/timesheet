package eu.openvalue.timesheet.adapter.out.draft;

import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.domain.EmailAddress;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps drafts in memory only, so they are lost when the application stops.
 */
public final class InMemoryTimesheetDrafts implements TimesheetDrafts {

  private final Map<Key, Timesheet> drafts = new ConcurrentHashMap<>();

  @Override
  public Optional<Timesheet> find(Professional professional, YearMonth month) {
    return Optional.ofNullable(drafts.get(new Key(professional.email(), month)));
  }

  @Override
  public void save(Timesheet draft) {
    drafts.put(new Key(draft.professional().email(), draft.month()), draft);
  }

  private record Key(EmailAddress professional, YearMonth month) {
  }
}
