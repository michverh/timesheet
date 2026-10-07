package eu.openvalue.timesheet.adapter.out.assignment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The assignment of the (single) professional, until assignments can be maintained in the application.
 *
 * @param client the client worked for; blank when on the bench
 * @param project the project worked on, if any
 */
@ConfigurationProperties("timesheet.assignment")
public record AssignmentProperties(String client, String project) {
}
