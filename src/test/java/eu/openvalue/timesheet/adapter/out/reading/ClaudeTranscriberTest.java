package eu.openvalue.timesheet.adapter.out.reading;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.json.JsonMapper;

class ClaudeTranscriberTest {

  private static final String MODEL = "claude-sonnet-5-5";
  private static final byte[] IMAGE = {1, 2, 3, 4};
  private static final String TRANSCRIPTION = "{\"year\": 2026, \"days\": [], \"totals\": []}";

  @RegisterExtension
  static final WireMockExtension ANTHROPIC = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  private ClaudeTranscriber transcriber;

  @BeforeEach
  void connect() {
    transcriber = ClaudeTranscriber.connectingTo(URI.create(ANTHROPIC.baseUrl()), Optional.of("test-key"), MODEL,
        Duration.ofSeconds(10));
  }

  @Test
  void asksTheModelToTranscribeTheImagesWithTheInstructions() {
    // Given
    answer(TRANSCRIPTION, "end_turn");

    // When
    transcriber.transcribe(request());

    // Then
    ANTHROPIC.verify(postRequestedFor(urlPathEqualTo("/v1/messages"))
        .withHeader("x-api-key", equalTo("test-key"))
        .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL)))
        .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("user")))
        .withRequestBody(matchingJsonPath("$.messages[0].content[0].type", equalTo("image")))
        .withRequestBody(matchingJsonPath("$.messages[0].content[0].source.media_type", equalTo("image/png")))
        .withRequestBody(matchingJsonPath("$.messages[0].content[0].source.data",
            equalTo(Base64.getEncoder().encodeToString(IMAGE))))
        .withRequestBody(matchingJsonPath("$.messages[0].content[1].text", equalTo("Transcribe this timesheet."))));
  }

  @Test
  void asksForAnAnswerInTheRequestedStructureWithLittleReasoning() {
    // Given
    answer(TRANSCRIPTION, "end_turn");

    // When
    transcriber.transcribe(request());

    // Then
    ANTHROPIC.verify(postRequestedFor(urlPathEqualTo("/v1/messages"))
        .withRequestBody(matchingJsonPath("$.output_config.effort", equalTo("low")))
        .withRequestBody(matchingJsonPath("$.output_config.format.type", equalTo("json_schema")))
        .withRequestBody(matchingJsonPath("$.output_config.format.schema.required[0]", equalTo("year")))
        .withRequestBody(matchingJsonPath("$.output_config.format.schema.additionalProperties", equalTo("false")))
        .withRequestBody(matchingJsonPath("$.output_config.format.schema.properties.days.items.additionalProperties",
            equalTo("false"))));
  }

  @Test
  void letsTheApiFallBackToAnotherModelWhenDeclined() {
    // Given
    answer(TRANSCRIPTION, "end_turn");

    // When
    transcriber.transcribe(request());

    // Then
    ANTHROPIC.verify(postRequestedFor(urlPathEqualTo("/v1/messages"))
        .withHeader("anthropic-beta", equalTo("server-side-fallback-2026-07-01"))
        .withRequestBody(matchingJsonPath("$.fallbacks", equalTo("default"))));
  }

  @Test
  void givesTheTranscription() {
    // Given
    answer(TRANSCRIPTION, "end_turn");

    // When / Then
    assertThat(transcriber.transcribe(request())).isEqualTo(TRANSCRIPTION);
  }

  @Test
  void reportsADeclinedTranscription() {
    // Given
    answer("", "refusal");

    // When / Then
    assertThatThrownBy(() -> transcriber.transcribe(request()))
        .isInstanceOf(ClientTimesheetUnreadableException.class)
        .hasMessageContaining("declined");
  }

  @Test
  void reportsATranscriptionThatWasCutOff() {
    // Given
    answer("{\"year\": 2026, \"days\": [", "max_tokens");

    // When / Then
    assertThatThrownBy(() -> transcriber.transcribe(request()))
        .isInstanceOf(ClientTimesheetUnreadableException.class)
        .hasMessageContaining("ran out of room");
  }

  @Test
  void reportsAnApiThatFails() {
    // Given
    ANTHROPIC.stubFor(post(urlPathEqualTo("/v1/messages")).willReturn(serverError()));

    // When / Then
    assertThatThrownBy(() -> transcriber.transcribe(request())).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsAnAnswerWithoutTranscription() {
    // Given
    answer("  ", "end_turn");

    // When / Then
    assertThatThrownBy(() -> transcriber.transcribe(request())).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void forbidsPropertiesTheSchemaDoesNotDescribeOnEveryObjectKeepingTheOrder() {
    // Given
    var item = new LinkedHashMap<String, Object>();
    item.put("type", "object");
    item.put("properties", Map.of("d", Map.of("type", "integer")));
    var schema = new LinkedHashMap<String, Object>();
    schema.put("type", "object");
    schema.put("properties", Map.of("days", Map.of("type", "array", "items", item)));

    // When
    var strict = ClaudeTranscriber.strict(schema);

    // Then
    assertThat(strict).containsEntry("additionalProperties", false);
    assertThat(strict.keySet()).containsExactly("type", "properties", "additionalProperties");
    assertThat(strict).extracting("properties").extracting("days").extracting("items")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("additionalProperties", false);
  }

  private static TranscriptionRequest request() {
    var schema = new LinkedHashMap<String, Object>();
    schema.put("type", "object");
    schema.put("properties", Map.of("year", Map.of("type", "integer"),
        "days", Map.of("type", "array", "items", Map.of("type", "object", "properties", Map.of()))));
    schema.put("required", List.of("year"));
    return new TranscriptionRequest("Transcribe this timesheet.",
        List.of(new TranscriptionRequest.Image("image/png", IMAGE)), schema);
  }

  /**
   * Lets the API answer as the Messages API does: the transcription as the text of the model's answer.
   */
  private static void answer(String text, String stopReason) {
    var message = Map.of(
        "id", "msg_test",
        "type", "message",
        "role", "assistant",
        "model", MODEL,
        "content", List.of(Map.of("type", "text", "text", text)),
        "stop_reason", stopReason,
        "usage", Map.of("input_tokens", 1500, "output_tokens", 250));
    ANTHROPIC.stubFor(post(urlPathEqualTo("/v1/messages"))
        .willReturn(okJson(JsonMapper.builder().build().writeValueAsString(message))));
  }
}
