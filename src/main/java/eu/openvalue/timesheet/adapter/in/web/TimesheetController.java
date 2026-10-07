package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.domain.DomainException;
import eu.openvalue.timesheet.domain.HourCategory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.FragmentsRendering;

@Controller
class TimesheetController {

  private static final String VIEW = "timesheet";
  private static final String DAY_ROW = "fragments/day-row :: day-row";
  private static final String DAY_UPDATED = "fragments/day-updated :: day-updated";
  private static final HourCategory[] CATEGORIES = HourCategory.values();

  private final PrepareTimesheetDraftUseCase prepareDraft;
  private final AdjustTimesheetDayUseCase adjustDay;
  private final ResetTimesheetDraftUseCase resetDraft;
  private final CurrentProfessional currentProfessional;
  private final Clock clock;

  TimesheetController(PrepareTimesheetDraftUseCase prepareDraft, AdjustTimesheetDayUseCase adjustDay,
      ResetTimesheetDraftUseCase resetDraft, CurrentProfessional currentProfessional, Clock clock) {
    this.prepareDraft = prepareDraft;
    this.adjustDay = adjustDay;
    this.resetDraft = resetDraft;
    this.currentProfessional = currentProfessional;
    this.clock = clock;
  }

  @GetMapping("/")
  String currentMonth() {
    return "redirect:/timesheets/" + YearMonth.now(clock);
  }

  @GetMapping("/timesheets/{month}")
  String timesheet(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, Model model) {
    var draft = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(currentProfessional.get(), month));
    model.addAttribute("page", TimesheetPage.of(draft));
    model.addAttribute("categories", CATEGORIES);
    return VIEW;
  }

  @PostMapping("/timesheets/{month}/days/{date}")
  FragmentsRendering adjustDay(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam MultiValueMap<String, String> parameters) {
    if (!YearMonth.from(date).equals(month)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, date + " is not part of " + month);
    }
    var input = DayForm.from(parameters);
    try {
      var draft = adjustDay.adjustDay(input.toCommand(currentProfessional.get(), date));
      var page = TimesheetPage.of(draft);
      return FragmentsRendering
          .fragment(DAY_ROW, Map.of("row", page.day(date).orElseThrow(), "categories", CATEGORIES))
          .fragment(DAY_UPDATED, Map.of("page", page, "categories", CATEGORIES, "oob", true))
          .build();
    } catch (InvalidDayInputException | DomainException e) {
      return FragmentsRendering
          .fragment(DAY_ROW,
              Map.of("row", DayRow.rejected(date, input, e.getMessage()), "categories", CATEGORIES))
          .build();
    }
  }

  /**
   * Prefills the month again, after which htmx reloads the page.
   */
  @PostMapping("/timesheets/{month}/reset")
  ResponseEntity<Void> resetDraft(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
    resetDraft.resetDraft(new ResetTimesheetDraftCommand(currentProfessional.get(), month));
    return ResponseEntity.noContent().header("HX-Refresh", "true").build();
  }
}
