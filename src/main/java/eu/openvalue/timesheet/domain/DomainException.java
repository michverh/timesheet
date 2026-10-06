package eu.openvalue.timesheet.domain;

/**
 * Signals that an operation would violate a business rule of the domain.
 */
public abstract class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }
}
