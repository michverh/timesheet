-- Leave filled in for hours that were expected, but not worked, stays marked until the professional confirms it.
alter table timesheet_day add column leave_suggested boolean not null default false;
