package eu.openvalue.timesheet.adapter.out.reading;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.json.JsonMapper;

class OllamaTranscriberTest {

  private static final String MODEL = "qwen3-vl:8b-instruct";
  private static final byte[] IMAGE = {1, 2, 3, 4};
  private static final Map<String, Object> SCHEMA = Map.of("type", "object", "required", List.of("year"));
  private static final TranscriptionRequest REQUEST = new TranscriptionRequest("Transcribe this timesheet.",
      List.of(new TranscriptionRequest.Image("image/png", IMAGE)), SCHEMA);

  @RegisterExtension
  static final WireMockExtension OLLAMA = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  private OllamaTranscriber transcriber;

  @BeforeEach
  void connect() {
    transcriber = OllamaTranscriber.connectingTo(URI.create(OLLAMA.baseUrl()), MODEL, Duration.ofSeconds(5));
  }

  @Test
  void asksTheModelForAnAnswerInTheRequestedStructure() {
    // Given
    answer("{}");

    // When
    transcriber.transcribe(REQUEST);

    // Then
    OLLAMA.verify(postRequestedFor(urlPathEqualTo("/api/generate"))
        .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL)))
        .withRequestBody(matchingJsonPath("$.prompt", equalTo("Transcribe this timesheet.")))
        .withRequestBody(matchingJsonPath("$.images[0]", equalTo(Base64.getEncoder().encodeToString(IMAGE))))
        .withRequestBody(matchingJsonPath("$.format", equalToJson("{\"type\": \"object\", \"required\": [\"year\"]}")))
        .withRequestBody(matchingJsonPath("$.stream", equalTo("false"))));
  }

  @Test
  void keepsTheModelLoadedAndAnswersDeterministicallyWithRoomForLargeDocuments() {
    // Given
    answer("{}");

    // When
    transcriber.transcribe(REQUEST);

    // Then
    OLLAMA.verify(postRequestedFor(urlPathEqualTo("/api/generate"))
        .withRequestBody(matchingJsonPath("$.keep_alive", equalTo("30m")))
        .withRequestBody(matchingJsonPath("$.options.temperature", equalTo("0")))
        .withRequestBody(matchingJsonPath("$.options.num_ctx", equalTo("8192"))));
  }

  @Test
  void givesTheModelsAnswer() {
    // Given
    answer("{\"year\": 2026, \"days\": []}");

    // When / Then
    assertThat(transcriber.transcribe(REQUEST)).isEqualTo("{\"year\": 2026, \"days\": []}");
  }

  @Test
  void reportsAModelThatCannotBeReached() {
    // Given
    OLLAMA.stubFor(post(urlPathEqualTo("/api/generate")).willReturn(serverError()));

    // When / Then
    assertThatThrownBy(() -> transcriber.transcribe(REQUEST)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsAModelThatDoesNotAnswer() {
    // Given
    OLLAMA.stubFor(post(urlPathEqualTo("/api/generate")).willReturn(okJson("{\"done\": true}")));

    // When / Then
    assertThatThrownBy(() -> transcriber.transcribe(REQUEST)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  /**
   * Lets Ollama answer as it does without streaming: the answer is a string inside its response.
   */
  private static void answer(String response) {
    var answer = JsonMapper.builder().build()
        .writeValueAsString(Map.of("model", MODEL, "response", response, "done", true));
    OLLAMA.stubFor(post(urlPathEqualTo("/api/generate")).willReturn(okJson(answer)));
  }
}
