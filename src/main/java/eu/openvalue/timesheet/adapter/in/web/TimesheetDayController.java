package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.domain.DomainException;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.FragmentsRendering;

/**
 * Adjustments of a single day, answered with htmx fragments: the day's row, plus the totals and warnings when the
 * adjustment was accepted.
 */
@Controller
class TimesheetDayController {

  private static final String DAY_ROW = "fragments/day-row :: day-row";
  private static final String DAY_UPDATED = "fragments/day-updated :: day-updated";
  private static final HourCategory[] CATEGORIES = HourCategory.values();

  private final AdjustTimesheetDayUseCase adjustDay;

  TimesheetDayController(AdjustTimesheetDayUseCase adjustDay) {
    this.adjustDay = adjustDay;
  }

  @PostMapping("/timesheets/{month}/days/{date}")
  FragmentsRendering adjustDay(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam MultiValueMap<String, String> parameters, Professional professional) {
    if (!YearMonth.from(date).equals(month)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, date + " is not part of " + month);
    }
    var input = DayForm.from(parameters);
    try {
      var page = TimesheetPage.of(adjustDay.adjustDay(input.toCommand(professional, date)));
      return FragmentsRendering
          .fragment(DAY_ROW, Map.of("row", page.day(date).orElseThrow(), "categories", CATEGORIES))
          .fragment(DAY_UPDATED, Map.of("page", page, "categories", CATEGORIES, "oob", true))
          .build();
    } catch (InvalidDayInputException | DomainException e) {
      return FragmentsRendering
          .fragment(DAY_ROW, Map.of("row", DayRow.rejected(date, input, e.getMessage()), "categories", CATEGORIES))
          .build();
    }
  }
}
