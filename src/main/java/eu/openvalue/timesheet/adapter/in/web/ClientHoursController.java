package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursCommand;
import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursUseCase;
import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Taking over the hours worked according to the client's timesheet, after which htmx reloads the page.
 */
@Controller
class ClientHoursController {

  private final TakeOverClientHoursUseCase takeOver;

  ClientHoursController(TakeOverClientHoursUseCase takeOver) {
    this.takeOver = takeOver;
  }

  @PostMapping("/timesheets/{month}/client-hours")
  ResponseEntity<Void> takeOver(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      Professional professional) {
    takeOver.takeOverClientHours(new TakeOverClientHoursCommand(professional, month));
    return ResponseEntity.noContent().header("HX-Refresh", "true").build();
  }
}
