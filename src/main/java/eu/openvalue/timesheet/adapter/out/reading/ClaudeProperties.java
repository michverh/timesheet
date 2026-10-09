package eu.openvalue.timesheet.adapter.out.reading;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param baseUrl where the Anthropic API is reachable
 * @param apiKey the API key; when blank, the credentials in the environment are used, such as
 *     {@code ANTHROPIC_API_KEY}
 * @param model the Claude model that reads client timesheets
 * @param effort how much the model reasons before answering; {@code none} for models that do not support setting
 *     it, such as Claude Haiku 4.5
 * @param timeout how long to wait for the API to answer
 */
@ConfigurationProperties("timesheet.client-timesheet-reader.claude")
public record ClaudeProperties(
    @DefaultValue("https://api.anthropic.com") URI baseUrl,
    String apiKey,
    @DefaultValue("claude-sonnet-5-5") String model,
    @DefaultValue("low") String effort,
    @DefaultValue("2m") Duration timeout) {

  /**
   * @return the effort to ask for, or empty when it is not to be set
   */
  public Optional<String> effortToSet() {
    return Optional.ofNullable(effort).filter(level -> !level.isBlank() && !level.equalsIgnoreCase("none"));
  }
}
