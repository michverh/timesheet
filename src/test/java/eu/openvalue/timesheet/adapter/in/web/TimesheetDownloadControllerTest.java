package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.openvalue.timesheet.application.port.in.DownloadTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDownload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(TimesheetDownloadController.class)
@Import(WebTestConfiguration.class)
class TimesheetDownloadControllerTest {

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private DownloadTimesheetUseCase download;

  @Test
  void downloadsTheTimesheetOfTheCurrentProfessionalAsAFileInItsOwnFormat() {
    // Given
    var content = new byte[] {1, 2, 3};
    when(download.download(new DownloadTimesheetCommand(JAN, MAY_2026))).thenReturn(new TimesheetDownload(
        "Urenverantwoording_2026-05_Jan_Jansen.ods", "application/vnd.oasis.opendocument.spreadsheet", content));

    // When
    var response = mvc.get().uri("/timesheets/2026-05/download");

    // Then
    assertThat(response).hasStatusOk()
        .hasContentType("application/vnd.oasis.opendocument.spreadsheet")
        .hasHeader("Content-Disposition", "attachment; filename=\"Urenverantwoording_2026-05_Jan_Jansen.ods\"; "
            + "filename*=UTF-8''Urenverantwoording_2026-05_Jan_Jansen.ods")
        .body().isEqualTo(content);
  }
}
