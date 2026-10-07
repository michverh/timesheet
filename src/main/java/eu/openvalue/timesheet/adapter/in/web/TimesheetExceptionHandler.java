package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Maps the errors that are not handled on the page itself to HTTP responses, whose text the page shows as an alert.
 */
@ControllerAdvice
class TimesheetExceptionHandler {

  @ExceptionHandler
  ResponseEntity<String> draftNotFound(TimesheetDraftNotFoundException exception) {
    return plainText(HttpStatus.NOT_FOUND,
        "This draft timesheet is no longer available. Reload the page to start again from the prefilled hours.");
  }

  @ExceptionHandler
  ResponseEntity<String> ruleViolated(DomainException exception) {
    return plainText(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
  }

  private static ResponseEntity<String> plainText(HttpStatus status, String message) {
    return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(message);
  }
}
