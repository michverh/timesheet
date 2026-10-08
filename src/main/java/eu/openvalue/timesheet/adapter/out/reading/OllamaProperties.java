package eu.openvalue.timesheet.adapter.out.reading;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param baseUrl where Ollama is reachable
 * @param model the vision model that reads client timesheets
 * @param timeout how long to wait for Ollama to connect and to answer
 */
@ConfigurationProperties("timesheet.client-timesheet-reader.ollama")
public record OllamaProperties(URI baseUrl, String model, @DefaultValue("3m") Duration timeout) {
}
