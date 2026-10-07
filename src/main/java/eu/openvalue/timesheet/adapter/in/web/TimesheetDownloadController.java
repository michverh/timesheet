package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.DownloadTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.domain.Professional;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
class TimesheetDownloadController {

  private final DownloadTimesheetUseCase download;

  TimesheetDownloadController(DownloadTimesheetUseCase download) {
    this.download = download;
  }

  @GetMapping("/timesheets/{month}/download")
  ResponseEntity<byte[]> download(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
      Professional professional) {
    var file = download.download(new DownloadTimesheetCommand(professional, month));
    var disposition = ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mediaType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(file.content());
  }
}
