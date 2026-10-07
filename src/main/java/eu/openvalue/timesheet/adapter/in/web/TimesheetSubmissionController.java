package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.CorrectTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.CorrectTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetUseCase;
import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Submitting a timesheet to HR and reopening it for correction, after each of which htmx reloads the page.
 */
@Controller
class TimesheetSubmissionController {

  private final SubmitTimesheetUseCase submit;
  private final CorrectTimesheetUseCase correct;

  TimesheetSubmissionController(SubmitTimesheetUseCase submit, CorrectTimesheetUseCase correct) {
    this.submit = submit;
    this.correct = correct;
  }

  @PostMapping("/timesheets/{month}/submission")
  ResponseEntity<Void> submit(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      Professional professional) {
    submit.submit(new SubmitTimesheetCommand(professional, month));
    return reloadPage();
  }

  @PostMapping("/timesheets/{month}/correction")
  ResponseEntity<Void> correct(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      Professional professional) {
    correct.reopenForCorrection(new CorrectTimesheetCommand(professional, month));
    return reloadPage();
  }

  private static ResponseEntity<Void> reloadPage() {
    return ResponseEntity.noContent().header("HX-Refresh", "true").build();
  }
}
