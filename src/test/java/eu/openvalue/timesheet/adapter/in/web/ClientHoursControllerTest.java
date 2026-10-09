package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursCommand;
import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ClientHoursController.class)
@Import(WebTestConfiguration.class)
class ClientHoursControllerTest {

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private TakeOverClientHoursUseCase takeOver;

  @Test
  void takesOverTheClientsHoursAndLetsThePageReload() {
    // When
    var response = mvc.post().uri("/timesheets/2026-05/client-hours");

    // Then
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT).hasHeader("HX-Refresh", "true");
    verify(takeOver).takeOverClientHours(new TakeOverClientHoursCommand(JAN, MAY_2026));
  }
}
