package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.out.TimesheetChangedConcurrentlyException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Keeps copies of timesheets in memory and, like a database, refuses to keep a timesheet that was changed by someone
 * else after it was found.
 */
final class FakeTimesheets implements Timesheets {

  private final Map<Key, Kept> kept = new HashMap<>();
  private final Map<Timesheet, Integer> versionsFound = new IdentityHashMap<>();
  private final Queue<Consumer<Timesheet>> concurrentChanges = new ArrayDeque<>();

  @Override
  public Optional<Timesheet> find(Professional professional, YearMonth month) {
    return Optional.ofNullable(kept.get(new Key(professional, month))).map(found -> {
      var timesheet = copyOf(found.timesheet());
      versionsFound.put(timesheet, found.version());
      return timesheet;
    });
  }

  @Override
  public void save(Timesheet timesheet) {
    var key = new Key(timesheet.professional(), timesheet.month());
    var change = concurrentChanges.poll();
    if (change != null && kept.containsKey(key)) {
      var changedByOthers = copyOf(kept.get(key).timesheet());
      change.accept(changedByOthers);
      kept.put(key, new Kept(changedByOthers, kept.get(key).version() + 1));
    }
    var version = Optional.ofNullable(kept.get(key)).map(Kept::version);
    if (!version.equals(Optional.ofNullable(versionsFound.get(timesheet)))) {
      throw new TimesheetChangedConcurrentlyException(timesheet.professional(), timesheet.month());
    }
    var next = version.orElse(0) + 1;
    kept.put(key, new Kept(copyOf(timesheet), next));
    versionsFound.put(timesheet, next);
  }

  /**
   * Lets someone else change the kept timesheet right before it is saved next.
   */
  void changeConcurrently(Consumer<Timesheet> change) {
    concurrentChanges.add(change);
  }

  private static Timesheet copyOf(Timesheet timesheet) {
    return Timesheet.restore(timesheet.month(), timesheet.professional(), timesheet.assignment(), timesheet.days(),
        timesheet.submittedOn(), timesheet.previouslySubmittedOn(),
        timesheet.clientTimesheets());
  }

  private record Key(Professional professional, YearMonth month) {
  }

  private record Kept(Timesheet timesheet, int version) {
  }
}
