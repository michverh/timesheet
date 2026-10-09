-- Timesheets are no longer prefilled from HR's absence registry.
alter table timesheet drop column registered_absences_unknown;
