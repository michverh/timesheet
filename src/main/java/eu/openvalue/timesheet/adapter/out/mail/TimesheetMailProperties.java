package eu.openvalue.timesheet.adapter.out.mail;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param from the address timesheets are sent from
 * @param to the addresses of HR that receive submitted timesheets
 */
@ConfigurationProperties("timesheet.mail")
public record TimesheetMailProperties(String from, List<String> to) {
}
