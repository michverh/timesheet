package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetUseCase;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Professional;
import java.io.IOException;
import java.time.YearMonth;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Uploading the client's own timesheet, after which htmx reloads the page.
 */
@Controller
class ClientTimesheetController {

  private final AttachClientTimesheetUseCase attach;

  ClientTimesheetController(AttachClientTimesheetUseCase attach) {
    this.attach = attach;
  }

  @PostMapping("/timesheets/{month}/client-timesheet")
  ResponseEntity<String> upload(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      @RequestParam("file") MultipartFile file, Professional professional) throws IOException {
    var fileName = fileNameWithoutPath(file.getOriginalFilename());
    var format = DocumentFormat.ofFileName(fileName);
    if (file.isEmpty()) {
      return rejected("The uploaded file is empty.");
    }
    if (format.isEmpty()) {
      return rejected("Upload the client timesheet as a PNG, JPG, PDF or XLSX file.");
    }
    var document = new ClientTimesheetDocument(fileName, format.get(), file.getBytes());
    attach.attach(new AttachClientTimesheetCommand(professional, month, document));
    return ResponseEntity.noContent().header("HX-Refresh", "true").build();
  }

  /**
   * Some browsers send the full path of an uploaded file, with forward or backward slashes.
   */
  private static String fileNameWithoutPath(String originalFilename) {
    var fileName = Objects.requireNonNullElse(originalFilename, "");
    return fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
  }

  private static ResponseEntity<String> rejected(String reason) {
    return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(reason);
  }
}
