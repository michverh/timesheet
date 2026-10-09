package eu.openvalue.timesheet.adapter.out.persistence;

import eu.openvalue.timesheet.application.port.out.TimesheetChangedConcurrentlyException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps timesheets in a relational database through JPA, mapping them explicitly to and from how they are stored.
 *
 * <p>Refuses to keep a timesheet that was changed by someone else after it was found, so that change is not undone.
 * To tell, it remembers which version it found each timesheet in. Timesheets are compared by identity, and forgotten
 * once nobody holds on to them anymore.
 */
public class JpaTimesheets implements Timesheets {

  private final TimesheetEntityRepository repository;
  private final Map<Timesheet, Long> versionsFound = Collections.synchronizedMap(new WeakHashMap<>());

  public JpaTimesheets(TimesheetEntityRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Timesheet> find(Professional professional, YearMonth month) {
    return repository.findByProfessionalEmailAndMonth(professional.email().value(), month.atDay(1))
        .map(this::restore);
  }

  /**
   * @throws TimesheetChangedConcurrentlyException when the timesheet was changed since it was found, or was started
   *     while another one was kept for the same professional and month
   */
  @Override
  @Transactional
  public void save(Timesheet timesheet) {
    var professional = timesheet.professional();
    var month = timesheet.month();
    var kept = repository.findForUpdateByProfessionalEmailAndMonth(professional.email().value(), month.atDay(1));
    if (!kept.map(TimesheetEntity::version).equals(Optional.ofNullable(versionsFound.get(timesheet)))) {
      throw new TimesheetChangedConcurrentlyException(professional, month);
    }
    var entity = kept.orElseGet(() -> new TimesheetEntity(professional, month));
    entity.copy(timesheet);
    insertOrUpdate(entity, timesheet);
    versionsFound.put(timesheet, entity.version());
  }

  private Timesheet restore(TimesheetEntity entity) {
    var timesheet = entity.restore();
    versionsFound.put(timesheet, entity.version());
    return timesheet;
  }

  /**
   * Writes the timesheet right away, so a timesheet started at the same time for the same professional and month is
   * noticed here.
   */
  private void insertOrUpdate(TimesheetEntity entity, Timesheet timesheet) {
    try {
      repository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException e) {
      throw new TimesheetChangedConcurrentlyException(timesheet.professional(), timesheet.month(), e);
    }
  }
}
