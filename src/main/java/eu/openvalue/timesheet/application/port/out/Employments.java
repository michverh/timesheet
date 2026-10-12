package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;

/**
 * What the company knows about the employment of its professionals.
 */
public interface Employments {

  LocalDate employedSince(Professional professional);
}
