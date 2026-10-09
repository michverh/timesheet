package eu.openvalue.timesheet.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Finds and keeps timesheets as they are kept in the database.
 */
public interface TimesheetEntityRepository extends JpaRepository<TimesheetEntity, Long> {

  /**
   * @param month the first day of the month
   */
  Optional<TimesheetEntity> findByProfessionalEmailAndMonth(String professionalEmail, LocalDate month);

  /**
   * Finds the timesheet to change it, keeping others from changing it until the transaction ends.
   *
   * @param month the first day of the month
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TimesheetEntity> findForUpdateByProfessionalEmailAndMonth(String professionalEmail, LocalDate month);
}
