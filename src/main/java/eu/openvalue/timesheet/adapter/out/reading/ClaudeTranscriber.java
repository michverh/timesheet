package eu.openvalue.timesheet.adapter.out.reading;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transcribes with a Claude model through the Anthropic API, using structured outputs so the answer always conforms
 * to the requested schema.
 */
public final class ClaudeTranscriber implements Transcriber {

  /** Ample room for a month of days, plus the model's reasoning. */
  private static final long MAXIMUM_ANSWER_TOKENS = 8000;

  /**
   * Lets the API retry a request the model declines on safety grounds with a fitting other model, instead of failing.
   */
  private static final String SERVER_SIDE_FALLBACK = "server-side-fallback-2026-07-01";

  private final AnthropicClient client;
  private final String model;

  private ClaudeTranscriber(AnthropicClient client, String model) {
    this.client = client;
    this.model = model;
  }

  /**
   * @param apiKey the API key, or empty to use the credentials from the environment, such as
   *     {@code ANTHROPIC_API_KEY}
   * @param timeout how long to wait for the API to answer
   */
  public static ClaudeTranscriber connectingTo(URI baseUrl, Optional<String> apiKey, String model,
      Duration timeout) {
    Objects.requireNonNull(baseUrl, "baseUrl");
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(timeout, "timeout");
    var client = AnthropicOkHttpClient.builder().fromEnv().baseUrl(baseUrl.toString()).timeout(timeout);
    apiKey.filter(key -> !key.isBlank()).ifPresent(client::apiKey);
    return new ClaudeTranscriber(client.build(), model);
  }

  @Override
  public String transcribe(TranscriptionRequest request) {
    var answer = send(messageFor(request));
    var stopReason = answer.stopReason();
    if (stopReason.filter(StopReason.REFUSAL::equals).isPresent()) {
      throw new ClientTimesheetUnreadableException(model + " declined to transcribe the timesheet");
    }
    if (stopReason.filter(StopReason.MAX_TOKENS::equals).isPresent()) {
      throw new ClientTimesheetUnreadableException(model + " ran out of room before finishing the transcription");
    }
    return textOf(answer);
  }

  private Message send(MessageCreateParams message) {
    try {
      return client.messages().create(message);
    } catch (AnthropicException e) {
      throw new ClientTimesheetUnreadableException(model + " could not transcribe the timesheet", e);
    }
  }

  private MessageCreateParams messageFor(TranscriptionRequest request) {
    return MessageCreateParams.builder()
        .model(model)
        .maxTokens(MAXIMUM_ANSWER_TOKENS)
        .addUserMessageOfBlockParams(contentOf(request))
        .outputConfig(OutputConfig.builder()
            .effort(OutputConfig.Effort.LOW)
            .format(JsonOutputFormat.builder()
                .schema(JsonOutputFormat.Schema.builder()
                    .additionalProperties(jsonValuesOf(strict(request.schema())))
                    .build())
                .build())
            .build())
        .putAdditionalHeader("anthropic-beta", SERVER_SIDE_FALLBACK)
        .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
        .build();
  }

  private static List<ContentBlockParam> contentOf(TranscriptionRequest request) {
    var content = new ArrayList<ContentBlockParam>();
    request.images().forEach(image -> content.add(ContentBlockParam.ofImage(ImageBlockParam.builder()
        .source(Base64ImageSource.builder()
            .mediaType(Base64ImageSource.MediaType.of(image.mediaType()))
            .data(Base64.getEncoder().encodeToString(image.content()))
            .build())
        .build())));
    content.add(ContentBlockParam.ofText(request.prompt()));
    return content;
  }

  private static String textOf(Message answer) {
    var text = answer.content().stream()
        .flatMap(block -> block.text().stream())
        .map(TextBlock::text)
        .collect(Collectors.joining());
    if (text.isBlank()) {
      throw new ClientTimesheetUnreadableException("The answer of the model holds no transcription");
    }
    return text;
  }

  /**
   * Structured outputs require every object in the schema to forbid properties it does not describe.
   */
  @SuppressWarnings("unchecked")
  static Map<String, Object> strict(Map<String, Object> schema) {
    var strict = new LinkedHashMap<String, Object>();
    schema.forEach((key, value) -> strict.put(key, value instanceof Map<?, ?> nested
        ? strict((Map<String, Object>) nested)
        : value));
    if ("object".equals(schema.get("type"))) {
      strict.put("additionalProperties", false);
    }
    return strict;
  }

  private static Map<String, JsonValue> jsonValuesOf(Map<String, Object> schema) {
    var values = new LinkedHashMap<String, JsonValue>();
    schema.forEach((key, value) -> values.put(key, JsonValue.from(value)));
    return values;
  }
}
