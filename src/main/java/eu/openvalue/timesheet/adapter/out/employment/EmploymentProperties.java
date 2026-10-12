package eu.openvalue.timesheet.adapter.out.employment;

import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The employment of the (single) professional, until HR maintains it in the application.
 *
 * @param startDate the first day of employment
 */
@ConfigurationProperties("timesheet.employment")
public record EmploymentProperties(LocalDate startDate) {
}
