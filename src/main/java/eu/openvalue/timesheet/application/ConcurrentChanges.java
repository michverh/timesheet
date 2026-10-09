package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.out.TimesheetChangedConcurrentlyException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Applies a change to a timesheet once more, to the timesheet as it is kept then, when another change got in between
 * finding and keeping it. Only for changes that can safely be applied again: without side effects outside the
 * timesheet.
 */
final class ConcurrentChanges {

  /**
   * Enough for a burst of changes, such as a professional quickly tabbing through a week; each attempt one of the
   * competing changes gets kept.
   */
  static final int ATTEMPTS = 10;

  /**
   * The longest pause before the next attempt grows with each attempt, and the actual pause is random, so competing
   * changes spread out instead of colliding again.
   */
  private static final long PAUSE_MILLIS_PER_ATTEMPT = 10;

  private ConcurrentChanges() {
  }

  static <T> T retried(Supplier<T> change) {
    var conflict = (TimesheetChangedConcurrentlyException) null;
    for (var attempt = 1; attempt <= ATTEMPTS; attempt++) {
      try {
        return change.get();
      } catch (TimesheetChangedConcurrentlyException e) {
        conflict = e;
        pauseAfter(attempt);
      }
    }
    throw conflict;
  }

  private static void pauseAfter(int attempt) {
    try {
      Thread.sleep(ThreadLocalRandom.current().nextLong(PAUSE_MILLIS_PER_ATTEMPT * attempt + 1));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static void retried(Runnable change) {
    retried(() -> {
      change.run();
      return null;
    });
  }
}
