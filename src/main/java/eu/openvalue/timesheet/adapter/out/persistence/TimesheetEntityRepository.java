package eu.openvalue.timesheet.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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

  /**
   * The holiday leave booked in each of the professional's timesheets from one month up to another.
   *
   * @param from the first day of the first month
   * @param until the first day of the month after the last
   */
  @Query(nativeQuery = true, value = """
      select t.month as month, t.submitted_on is not null as submitted, coalesce(sum(h.minutes), 0) as minutes
      from timesheet t
      join timesheet_day d on d.timesheet_id = t.id
      left join timesheet_day_hours h on h.timesheet_day_id = d.id and h.category = 'LEAVE'
      where t.professional_email = :professionalEmail and t.month >= :from and t.month < :until
      group by t.month, t.submitted_on
      order by t.month""")
  List<LeaveBookedInMonth> leaveBookedBetween(String professionalEmail, LocalDate from, LocalDate until);

  /**
   * The holiday leave booked in a timesheet.
   */
  interface LeaveBookedInMonth {

    LocalDate getMonth();

    boolean getSubmitted();

    long getMinutes();
  }
}
