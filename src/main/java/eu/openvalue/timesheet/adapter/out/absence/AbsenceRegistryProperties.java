package eu.openvalue.timesheet.adapter.out.absence;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param baseUrl where HR's absence registry is reachable
 * @param timeout how long to wait for the registry to connect and to answer
 */
@ConfigurationProperties("timesheet.absence-registry")
public record AbsenceRegistryProperties(URI baseUrl, @DefaultValue("5s") Duration timeout) {
}
