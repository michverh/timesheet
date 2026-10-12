package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsCommand;
import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsUseCase;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(ClientTimesheetController.class)
@Import(WebTestConfiguration.class)
class ClientTimesheetControllerTest {

  private static final byte[] CONTENT = {1, 2, 3};

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private ImportClientTimesheetsUseCase importing;

  @Test
  void importsTheUploadedFilesAsTheClientsTimesheetsAndLetsThePageReload() {
    // When
    var response = upload(
        new MockMultipartFile("files", "week-19.png", "image/png", CONTENT),
        new MockMultipartFile("files", "week-20.pdf", "application/pdf", CONTENT));

    // Then
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT).hasHeader("HX-Refresh", "true");
    verify(importing).importClientTimesheets(new ImportClientTimesheetsCommand(JAN, MAY_2026, List.of(
        new ClientTimesheetDocument("week-19.png", DocumentFormat.PNG, CONTENT),
        new ClientTimesheetDocument("week-20.pdf", DocumentFormat.PDF, CONTENT))));
  }

  @Test
  void keepsOnlyTheNameOfAFileUploadedWithItsPath() {
    // When
    upload(new MockMultipartFile("files", "C:\\Users\\jan\\acme-may.png", "image/png", CONTENT));

    // Then
    verify(importing).importClientTimesheets(new ImportClientTimesheetsCommand(JAN, MAY_2026,
        List.of(new ClientTimesheetDocument("acme-may.png", DocumentFormat.PNG, CONTENT))));
  }

  @Test
  void rejectsAnEmptyFile() {
    // When
    var response = upload(
        new MockMultipartFile("files", "week-19.png", "image/png", CONTENT),
        new MockMultipartFile("files", "week-20.pdf", "application/pdf", new byte[0]));

    // Then
    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).bodyText().contains("week-20.pdf is empty");
    verify(importing, never()).importClientTimesheets(any());
  }

  @Test
  void rejectsAFileOfAnotherFormat() {
    // When
    var response = upload(new MockMultipartFile("files", "acme-may.docx", "application/msword", CONTENT));

    // Then
    assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).bodyText()
        .contains("acme-may.docx as a PNG, JPG, PDF or XLSX");
    verify(importing, never()).importClientTimesheets(any());
  }

  @Test
  void explainsTheLimitOfAFileThatIsTooLarge() {
    // Given
    doThrow(new MaxUploadSizeExceededException(10 * 1024 * 1024)).when(importing).importClientTimesheets(any());

    // When
    var response = upload(new MockMultipartFile("files", "huge.pdf", "application/pdf", CONTENT));

    // Then
    assertThat(response).hasStatus(HttpStatus.CONTENT_TOO_LARGE).bodyText().contains("at most 10 MB");
  }

  private MvcTestResult upload(MockMultipartFile... files) {
    var request = mvc.post().uri("/timesheets/2026-05/client-timesheet").multipart();
    for (var file : files) {
      request = request.file(file);
    }
    return request.exchange();
  }
}
