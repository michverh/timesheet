package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheet;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DayEntry;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Project;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the timesheet page shows of a draft timesheet.
 */
public record TimesheetPage(String professionalName, String professionalEmail, YearMonth month, String monthLabel,
    String client, String project, List<DayRow> days, Map<HourCategory, String> totalHours, String totalKilometres,
    List<String> daysLackingExplanation, Optional<String> submittedOn, Optional<String> previouslySubmittedOn,
    boolean registeredAbsencesUnknown, Optional<String> clientTimesheetFileName, boolean lacksClientTimesheet,
    boolean clientTimesheetUnreadable, Optional<String> clientTimesheetPeriod,
    boolean clientTimesheetOfAnotherPeriod, List<String> daysDeviatingFromStandardWorkday) {

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
        timesheet.registeredAbsencesUnknown(),
        timesheet.clientTimesheet().map(ClientTimesheet::document).map(ClientTimesheetDocument::fileName),
        timesheet.lacksClientTimesheet(),
        timesheet.clientTimesheet().filter(ClientTimesheet::isUnreadable).isPresent(),
        hoursReportedByClient(timesheet).flatMap(TimesheetPage::periodOf),
        timesheet.isClientTimesheetOfAnotherPeriod(),
        timesheet.daysDeviatingFromStandardWorkday().stream().map(TimesheetPage::describe).toList());
  }

  private static List<DayRow> rowsOf(Timesheet timesheet) {
    return timesheet.days().stream().map(DayRow::of).toList();
  }

  private static Optional<ClientReportedHours> hoursReportedByClient(Timesheet timesheet) {
    return timesheet.clientTimesheet().flatMap(ClientTimesheet::hoursReportedByClient);
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

  private static String describe(DayEntry day) {
    return "%s: %s instead of %s".formatted(DAY_LABEL.format(day.date()), HoursFormat.format(day.workdayHours()),
        HoursFormat.format(DayEntry.STANDARD_WORKDAY));
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
