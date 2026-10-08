package eu.openvalue.timesheet.adapter.out.reading;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Transcribes with a model served by Ollama, for instance a local vision model.
 */
public final class OllamaTranscriber implements Transcriber {

  /**
   * Keeps the model loaded between documents, instead of loading it again for every upload.
   */
  private static final String KEEP_LOADED = "30m";

  /**
   * Room for a spreadsheet written out as text plus the answer; Ollama's default would silently cut larger ones off.
   */
  private static final int CONTEXT_TOKENS = 8192;

  private final RestClient restClient;
  private final String model;

  private OllamaTranscriber(RestClient restClient, String model) {
    this.restClient = restClient;
    this.model = model;
  }

  /**
   * @param timeout how long to wait for Ollama to connect, and to answer; a local model can take minutes
   */
  public static OllamaTranscriber connectingTo(URI baseUrl, String model, Duration timeout) {
    Objects.requireNonNull(baseUrl, "baseUrl");
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(timeout, "timeout");
    var httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(timeout)
        .build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(timeout);
    return new OllamaTranscriber(
        RestClient.builder().baseUrl(baseUrl.toString()).requestFactory(requestFactory).build(), model);
  }

  @Override
  public String transcribe(TranscriptionRequest request) {
    try {
      return generate(generateRequestFor(request)).response();
    } catch (RestClientException e) {
      throw new ClientTimesheetUnreadableException("Ollama could not transcribe with " + model, e);
    }
  }

  private GenerateRequest generateRequestFor(TranscriptionRequest request) {
    var images = request.images().stream()
        .map(image -> Base64.getEncoder().encodeToString(image.content()))
        .toList();
    return new GenerateRequest(model, request.prompt(), images, false, request.schema(), KEEP_LOADED,
        Map.of("temperature", 0, "num_ctx", CONTEXT_TOKENS));
  }

  private GenerateResponse generate(GenerateRequest request) {
    var response = restClient.post().uri("/api/generate")
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .body(GenerateResponse.class);
    if (response == null || response.response() == null) {
      throw new RestClientException("Ollama answered without a response");
    }
    return response;
  }

  record GenerateRequest(String model, String prompt, List<String> images, boolean stream,
      Map<String, Object> format, @JsonProperty("keep_alive") String keepAlive,
      Map<String, Object> options) {
  }

  record GenerateResponse(String response) {
  }
}
