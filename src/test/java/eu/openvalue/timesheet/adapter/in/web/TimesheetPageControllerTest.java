package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.draft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(TimesheetPageController.class)
@Import(WebTestConfiguration.class)
class TimesheetPageControllerTest {

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PrepareTimesheetDraftUseCase prepareDraft;

  @MockitoBean
  private ResetTimesheetDraftUseCase resetDraft;

  @Test
  void opensTheTimesheetOfTheCurrentMonth() {
    assertThat(mvc.get().uri("/")).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/timesheets/2026-05");
  }

  @Test
  void showsTheDraftOfTheCurrentProfessional() {
    // Given
    when(prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026))).thenReturn(draft());

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("Jan Jansen", "May 2026", "ACME", "Platform")
        .contains("id=\"day-2026-05-31\"")
        .containsPattern("id=\"2026-05-04-WORKED\"\\s+name=\"hours-WORKED\"\\s+value=\"8\"");
  }

  @Test
  void prefillsTheMonthAgainAndLetsThePageReload() {
    // When
    var response = mvc.post().uri("/timesheets/2026-05/reset");

    // Then
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT).hasHeader("HX-Refresh", "true");
    verify(resetDraft).resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));
  }
}
