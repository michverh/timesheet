package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Professional;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * The page of a month's draft timesheet as a whole.
 */
@Controller
class TimesheetPageController {

  private final PrepareTimesheetDraftUseCase prepareDraft;
  private final ResetTimesheetDraftUseCase resetDraft;
  private final Clock clock;

  TimesheetPageController(PrepareTimesheetDraftUseCase prepareDraft, ResetTimesheetDraftUseCase resetDraft,
      Clock clock) {
    this.prepareDraft = prepareDraft;
    this.resetDraft = resetDraft;
    this.clock = clock;
  }

  @GetMapping("/")
  String currentMonth() {
    return "redirect:/timesheets/" + YearMonth.now(clock);
  }

  @GetMapping("/timesheets/{month}")
  String timesheet(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, Professional professional,
      Model model) {
    var draft = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(professional, month));
    model.addAttribute("page", TimesheetPage.of(draft));
    model.addAttribute("categories", HourCategory.values());
    return "timesheet";
  }

  /**
   * Prefills the month again, after which htmx reloads the page.
   */
  @PostMapping("/timesheets/{month}/reset")
  ResponseEntity<Void> reset(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      Professional professional) {
    resetDraft.resetDraft(new ResetTimesheetDraftCommand(professional, month));
    return ResponseEntity.noContent().header("HX-Refresh", "true").build();
  }
}
