package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsCommand;
import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsUseCase;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Professional;
import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
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
 * Uploading the client's own timesheets for a month in one go, typically one per week, after which htmx reloads the
 * page.
 */
@Controller
class ClientTimesheetController {

  private final ImportClientTimesheetsUseCase importing;

  ClientTimesheetController(ImportClientTimesheetsUseCase importing) {
    this.importing = importing;
  }

  @PostMapping("/timesheets/{month}/client-timesheet")
  ResponseEntity<String> upload(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      @RequestParam("files") List<MultipartFile> files, Professional professional) throws IOException {
    var documents = new ArrayList<ClientTimesheetDocument>();
    for (var file : files) {
      var fileName = fileNameWithoutPath(file.getOriginalFilename());
      var format = DocumentFormat.ofFileName(fileName);
      if (file.isEmpty()) {
        return rejected("%s is empty.".formatted(fileName));
      }
      if (format.isEmpty()) {
        return rejected("Upload %s as a PNG, JPG, PDF or XLSX file.".formatted(fileName));
      }
      documents.add(new ClientTimesheetDocument(fileName, format.get(), file.getBytes()));
    }
    if (documents.isEmpty()) {
      return rejected("Choose the client's timesheets to upload.");
    }
    importing.importClientTimesheets(new ImportClientTimesheetsCommand(professional, month, documents));
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
