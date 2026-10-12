package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The client's own timesheets imported for a month, typically one per week, and what they report together: the hours
 * worked on each day they cover, unless they disagree about it.
 */
public final class ClientTimesheets {

  public static final ClientTimesheets NONE = new ClientTimesheets(List.of());

  private final List<ClientTimesheet> timesheets;

  private ClientTimesheets(List<ClientTimesheet> timesheets) {
    this.timesheets = List.copyOf(timesheets);
  }

  /**
   * @throws IllegalArgumentException when the same document is imported more than once
   */
  public static ClientTimesheets of(List<ClientTimesheet> timesheets) {
    Objects.requireNonNull(timesheets, "timesheets");
    if (timesheets.stream().map(ClientTimesheet::document).distinct().count() < timesheets.size()) {
      throw new IllegalArgumentException("The same client's timesheet was imported more than once");
    }
    return new ClientTimesheets(timesheets);
  }

  public List<ClientTimesheet> all() {
    return timesheets;
  }

  public boolean isEmpty() {
    return timesheets.isEmpty();
  }

  public List<ClientTimesheetDocument> documents() {
    return timesheets.stream().map(ClientTimesheet::document).toList();
  }

  public List<ClientTimesheet> unreadable() {
    return timesheets.stream().filter(ClientTimesheet::isUnreadable).toList();
  }

  /**
   * The client's timesheets that were read, but cover none of the days of the month.
   */
  public List<ClientTimesheet> ofAnotherPeriodThan(YearMonth month) {
    return timesheets.stream().filter(timesheet -> timesheet.isOfAnotherPeriodThan(month)).toList();
  }

  /**
   * The hours worked on each day of the month that the client's timesheets agree on.
   */
  SortedMap<LocalDate, Hours> hoursWorkedIn(YearMonth month) {
    var agreed = new TreeMap<LocalDate, Hours>();
    reportedIn(month).forEach((date, reported) -> {
      if (reported.size() == 1) {
        agreed.put(date, reported.first());
      }
    });
    return agreed;
  }

  /**
   * The days of the month on which the client's timesheets report different hours worked.
   */
  public SortedSet<LocalDate> daysInDisputeIn(YearMonth month) {
    var disputed = new TreeSet<LocalDate>();
    reportedIn(month).forEach((date, reported) -> {
      if (reported.size() > 1) {
        disputed.add(date);
      }
    });
    return disputed;
  }

  /**
   * Whether any of the client's timesheets that were read reports on the day.
   */
  public boolean cover(LocalDate date) {
    return timesheets.stream()
        .flatMap(timesheet -> timesheet.hoursReportedByClient().stream())
        .anyMatch(reported -> reported.on(date).isPresent());
  }

  /**
   * The different hours worked reported on each day of the month.
   */
  private SortedMap<LocalDate, SortedSet<Hours>> reportedIn(YearMonth month) {
    var reported = new TreeMap<LocalDate, SortedSet<Hours>>();
    timesheets.stream()
        .flatMap(timesheet -> timesheet.hoursReportedByClient().stream())
        .flatMap(hours -> hours.hoursWorkedPerDay().entrySet().stream())
        .filter(day -> YearMonth.from(day.getKey()).equals(month))
        .forEach(day -> reported.computeIfAbsent(day.getKey(), date -> new TreeSet<>()).add(day.getValue()));
    return reported;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ClientTimesheets that && timesheets.equals(that.timesheets);
  }

  @Override
  public int hashCode() {
    return timesheets.hashCode();
  }

  @Override
  public String toString() {
    return "ClientTimesheets" + timesheets;
  }
}
