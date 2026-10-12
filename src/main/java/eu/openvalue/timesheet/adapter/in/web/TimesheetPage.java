package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheet;
import eu.openvalue.timesheet.domain.DayEntry;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Project;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.WorkWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything the timesheet page shows of a draft timesheet.
 */
public record TimesheetPage(String professionalName, String professionalEmail, YearMonth month, String monthLabel,
    String client, String project, List<DayRow> days, Map<HourCategory, String> totalHours, String totalKilometres,
    List<String> daysLackingExplanation, Optional<String> submittedOn, Optional<String> previouslySubmittedOn,
    List<ClientTimesheetRow> clientTimesheets, boolean lacksClientTimesheet,
    List<String> daysInDisputeBetweenClientTimesheets, List<String> weekdaysNotCoveredByClientTimesheets,
    List<String> weeksDeviatingFromStandardWorkWeek, List<String> daysWithSuggestedLeave) {

  private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);
  private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter DAY_AND_MONTH_LABEL = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);
  private static final DateTimeFormatter DAY_LABEL_ONLY = DateTimeFormatter.ofPattern("d", Locale.ENGLISH);

  static TimesheetPage of(Timesheet timesheet) {
    var assignment = timesheet.assignment();
    var totalHours = new EnumMap<HourCategory, String>(HourCategory.class);
    for (var category : HourCategory.values()) {
      totalHours.put(category, DayRow.format(timesheet.totalHours(category)));
    }
    return new TimesheetPage(
        timesheet.professional().name(),
        timesheet.professional().email().value(),
        timesheet.month(),
        MONTH_LABEL.format(timesheet.month()),
        assignment.map(Assignment::client).map(Client::name).orElse(""),
        assignment.flatMap(Assignment::project).map(Project::name).orElse(""),
        rowsOf(timesheet),
        totalHours,
        DayRow.format(timesheet.totalKilometres()),
        timesheet.daysLackingRequiredExplanation().stream().map(DAY_LABEL::format).toList(),
        timesheet.submittedOn().map(DATE_LABEL::format),
        timesheet.previouslySubmittedOn().map(DATE_LABEL::format),
        timesheet.clientTimesheets().all().stream().map(client -> rowOf(client, timesheet.month())).toList(),
        timesheet.lacksClientTimesheet(),
        timesheet.daysInDisputeBetweenClientTimesheets().stream().map(DAY_LABEL::format).toList(),
        timesheet.weekdaysNotCoveredByClientTimesheets().stream().map(DAY_LABEL::format).toList(),
        timesheet.weeksDeviatingFromStandardWorkWeek().stream().map(TimesheetPage::describe).toList(),
        timesheet.daysWithSuggestedLeave().stream().map(DAY_LABEL::format).toList());
  }

  private static List<DayRow> rowsOf(Timesheet timesheet) {
    var asReportedByClient = Set.copyOf(timesheet.daysBookedAsReportedByClient());
    var inDeviatingWeeks = timesheet.weeksDeviatingFromStandardWorkWeek().stream()
        .flatMap(week -> week.days().stream())
        .map(DayEntry::date)
        .collect(Collectors.toSet());
    return timesheet.days().stream()
        .map(day -> DayRow.of(day, asReportedByClient.contains(day.date()), inDeviatingWeeks.contains(day.date())))
        .toList();
  }

  private static ClientTimesheetRow rowOf(ClientTimesheet client, YearMonth month) {
    return new ClientTimesheetRow(client.document().fileName(),
        client.hoursReportedByClient().flatMap(TimesheetPage::periodOf), client.isUnreadable(),
        client.isOfAnotherPeriodThan(month));
  }

  /**
   * The days a client's timesheet covers, as compactly as reads naturally: "1 – 31 August 2026", "29 June – 5 July
   * 2026" or "29 December 2026 – 4 January 2027".
   */
  private static Optional<String> periodOf(ClientReportedHours reported) {
    return reported.firstDay().flatMap(first -> reported.lastDay().map(last -> periodBetween(first, last)));
  }

  private static String periodBetween(LocalDate first, LocalDate last) {
    if (first.equals(last)) {
      return DATE_LABEL.format(first);
    }
    var firstLabel = first.getYear() != last.getYear() ? DATE_LABEL
        : first.getMonth() != last.getMonth() ? DAY_AND_MONTH_LABEL
        : DAY_LABEL_ONLY;
    return firstLabel.format(first) + " – " + DATE_LABEL.format(last);
  }

  private static String describe(WorkWeek week) {
    return "Week %d (%s): %s instead of %s".formatted(week.number(), periodBetween(week.firstDay(), week.lastDay()),
        HoursFormat.format(week.bookedHours()), HoursFormat.format(week.standardHours()));
  }

  Optional<DayRow> day(LocalDate date) {
    return days.stream().filter(row -> row.date().equals(date)).findFirst();
  }

  public YearMonth previousMonth() {
    return month.minusMonths(1);
  }

  public YearMonth nextMonth() {
    return month.plusMonths(1);
  }

  public boolean submitted() {
    return submittedOn.isPresent();
  }

  /**
   * Whether the timesheet was reopened to correct an earlier submission, and has not been submitted again yet.
   */
  public boolean beingCorrected() {
    return !submitted() && previouslySubmittedOn.isPresent();
  }

  public boolean onBench() {
    return client.isEmpty();
  }
}
