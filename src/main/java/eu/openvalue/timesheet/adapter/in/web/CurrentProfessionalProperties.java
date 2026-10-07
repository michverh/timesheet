package eu.openvalue.timesheet.adapter.in.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param email the company email address of the professional using the application
 */
@ConfigurationProperties("timesheet.professional")
public record CurrentProfessionalProperties(String email) {
}
